package com.wavehub;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.transaction.AfterTransaction;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("kingbase")
@WithMockUser(roles = "ADMIN")
@Transactional
@EnabledIfEnvironmentVariable(named = "WAVEHUB_DB_TEST", matches = "true")
class KingbaseStationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    String prefix;

    @BeforeEach
    void verifyTargetBeforeWriting() {
        assertEquals("WaveHubDB", jdbc.queryForObject("SELECT current_database()", String.class), "Refusing to test writes in another database");
        prefix = "WH_TEST_" + UUID.randomUUID().toString().replace("-", "");
    }

    @AfterTransaction
    void verifyTestDataWasRolledBack() {
        if (prefix == null) return;
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM station WHERE code IN (?, ?)", Integer.class, prefix, prefix + "_B"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM waveform_file WHERE root_key = ?", Integer.class, prefix));
    }

    String body(String code, String name) {
        return """
                {"code":"%s","name":"%s","longitude":116.1234567,"latitude":40.1234567,
                 "elevationM":12.345,"enabled":true,"description":null}
                """.formatted(code, name);
    }

    String id(MvcResult result) throws Exception {
        return ((Map<?, ?>) json.readValue(result.getResponse().getContentAsString(), Map.class).get("data")).get("id").toString();
    }

    String createStation(String code) throws Exception {
        return id(mvc.perform(post("/api/stations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(code, "测试台站")))
                .andExpect(status().isCreated()).andReturn());
    }

    @Test
    void realDatabaseCrudValidationAndReferences() throws Exception {
        String station = createStation(prefix);
        String otherStation = createStation(prefix + "_B");
        mvc.perform(get("/api/stations").param("keyword", prefix)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
        mvc.perform(post("/api/stations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(prefix, "重复")))
                .andExpect(status().isConflict());
        String sensorPath = "/api/stations/" + station + "/sensors";
        String sensor = id(mvc.perform(post(sensorPath).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body("S01", "测试传感器")))
                .andExpect(status().isCreated()).andReturn());
        for (int i = 2; i <= 3; i++) mvc.perform(post(sensorPath).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body("S0" + i, "测试传感器"))).andExpect(status().isCreated());
        mvc.perform(get("/api/stations/" + station)).andExpect(jsonPath("$.data.sensorCount").value(3));
        mvc.perform(get(sensorPath)).andExpect(jsonPath("$.data.total").value(3));
        mvc.perform(post(sensorPath).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body("S01", "重复")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/stations/" + otherStation + "/sensors").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body("S01", "另一站传感器"))).andExpect(status().isCreated());
        mvc.perform(get("/api/stations/" + otherStation + "/sensors/" + sensor)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/stations/" + station).with(csrf())).andExpect(status().isConflict());
        String update = body("S01", "更新传感器").replace("\"enabled\":true", "\"enabled\":false");
        mvc.perform(put(sensorPath + "/" + sensor).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.model").isEmpty());
        assertEquals(0, jdbc.queryForObject("SELECT enabled FROM sensor WHERE id = ?", Integer.class, Long.valueOf(sensor)));

        long file = IdWorker.getId(), trace = IdWorker.getId();
        jdbc.update("INSERT INTO waveform_file (id, root_key, relative_path, file_name, format_code) VALUES (?, ?, ?, ?, ?)",
                file, prefix, "reference.wav", "reference.wav", "WAV");
        jdbc.update("INSERT INTO waveform_trace (id, file_id, sensor_id, trace_key) VALUES (?, ?, ?, ?)", trace, file, Long.valueOf(sensor), "test");
        mvc.perform(delete(sensorPath + "/" + sensor).with(csrf())).andExpect(status().isConflict());
        jdbc.update("DELETE FROM waveform_trace WHERE id = ?", trace);
        jdbc.update("DELETE FROM waveform_file WHERE id = ?", file);
        mvc.perform(delete(sensorPath + "/" + sensor).with(csrf())).andExpect(status().isOk());
        mvc.perform(get(sensorPath + "/" + sensor)).andExpect(status().isNotFound());
    }

    @Test
    void invalidInputsAreRejectedAndOptionalFieldsCanBeCleared() throws Exception {
        mvc.perform(post("/api/stations").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body(prefix, "坐标错误").replace("116.1234567", "181"))).andExpect(status().isBadRequest());
        String station = createStation(prefix);
        mvc.perform(put("/api/stations/" + station).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body(prefix, "空高程").replace("12.345", "null")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.elevationM").isEmpty());
        mvc.perform(post("/api/stations/" + station + "/sensors").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body("BAD", "采样率错误").replace("\"description\":null", "\"description\":null,\"nominalSampleRateHz\":0")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/stations/" + station + "/sensors").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body("BAD", "参数错误").replace("\"description\":null", "\"description\":null,\"parameters\":[]")))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/stations/" + station).with(csrf())).andExpect(status().isOk());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM station WHERE id = ?", Integer.class, Long.valueOf(station)));
    }
}
