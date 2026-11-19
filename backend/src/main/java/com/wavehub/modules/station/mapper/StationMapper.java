package com.wavehub.modules.station.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wavehub.modules.station.entity.Station;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.context.annotation.Profile;

@Mapper
@Profile("kingbase")
public interface StationMapper extends BaseMapper<Station> {}
