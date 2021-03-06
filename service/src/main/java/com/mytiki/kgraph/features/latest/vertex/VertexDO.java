/*
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

package com.mytiki.kgraph.features.latest.vertex;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.time.ZonedDateTime;

public abstract class VertexDO implements Serializable {
    @Id
    private String id;

    private ZonedDateTime created;

    private ZonedDateTime modified;

    @Transient
    private final String collection;

    public VertexDO(String collection) {
        this.collection = collection;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public ZonedDateTime getModified() {
        return modified;
    }

    public void setModified(ZonedDateTime modified) {
        this.modified = modified;
    }

    public void setCreated(ZonedDateTime created) {
        this.created = created;
    }

    public String getCollection() {
        return collection;
    }

    public String getDbId(){
        return collection + "/" + id;
    }

    @Override
    public String toString() {
        return "{" +
                "id='" + id + '\'' +
                ", collection='" + collection + '\'' +
                ", created='" + created + '\'' +
                ", modified='" + modified + '\'' +
                "}";
    }
}
