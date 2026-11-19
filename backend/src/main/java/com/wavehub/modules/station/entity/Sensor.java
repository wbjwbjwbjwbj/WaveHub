package com.wavehub.modules.station.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("sensor")
public class Sensor {
    @TableId(type = IdType.ASSIGN_ID)
    public Long id;
    public Long stationId;
    public String code;
    public String name;
    public BigDecimal longitude;
    public BigDecimal latitude;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    public BigDecimal elevationM;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    public String sensorType;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    public String model;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    public String serialNumber;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    public BigDecimal nominalSampleRateHz;
    public Integer parameterSchemaVersion;
    public String parametersJson;
    public Integer enabled;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    public String description;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
}
