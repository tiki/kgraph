/*
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

package com.mytiki.kgraph.features.latest.graph;

import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.Set;

@Edge(GraphEdgeDO.COLLECTION_NAME)
public class GraphEdgeDO<F extends GraphVertexDO, T extends GraphVertexDO> implements Serializable {
    public static final String COLLECTION_NAME = "edge";

    @Id
    private String id;

    @From
    private F from;

    @To
    private T to;

    private Set<String> fingerprints;

    private Integer qty;

    public GraphEdgeDO() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public F getFrom() {
        return from;
    }

    public void setFrom(F from) {
        this.from = from;
    }

    public T getTo() {
        return to;
    }

    public void setTo(T to) {
        this.to = to;
    }

    public Set<String> getFingerprints() {
        return fingerprints;
    }

    public void setFingerprints(Set<String> fingerprints) {
        this.fingerprints = fingerprints;
        if(fingerprints != null)
            this.qty = fingerprints.size();
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    @Override
    public String toString() {
        return "{" +
                "id='" + id + '\'' +
                ", from='" + from.toString() + '\'' +
                ", to='" + to.toString() + '\'' +
                ", qty='" + qty.toString() + '\'' +
                ", fingerprints='" + fingerprints.toString() + '\'' +
                "}";
    }
}