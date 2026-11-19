package com.wavehub.modules.station.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@TableName("station")
public class Station {
    @TableId(type = IdType.ASSIGN_ID)
    public Long id;
    public String code;
    public String name;
    public BigDecimal longitude;
    public BigDecimal latitude;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    public BigDecimal elevationM;
    public Integer enabled;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    public String description;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
}
