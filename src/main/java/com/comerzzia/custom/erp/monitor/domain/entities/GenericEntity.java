package com.comerzzia.custom.erp.monitor.domain.entities;

import com.comerzzia.custom.erp.monitor.domain.Entity;

public class GenericEntity extends Entity {
    private static final long serialVersionUID = 1L;
    
    // Tipo de entidad (para distinguir entre los tipos)
    private String entityType;

    public GenericEntity() {
        super();
    }

    public GenericEntity(String name, String baseFolder, int priority, String entityType) {
        super(name, baseFolder, priority);
        this.entityType = entityType;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
}