package com.wavehub.modules.station.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wavehub.modules.station.entity.Sensor;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.context.annotation.Profile;

@Mapper
@Profile("kingbase")
public interface SensorMapper extends BaseMapper<Sensor> {}
