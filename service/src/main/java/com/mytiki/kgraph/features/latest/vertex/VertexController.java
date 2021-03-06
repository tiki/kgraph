/*
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

package com.mytiki.kgraph.features.latest.vertex;

import com.mytiki.common.ApiConstants;
import com.mytiki.common.reply.ApiReplyAO;
import com.mytiki.common.reply.ApiReplyAOFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = VertexController.PATH_CONTROLLER)
public class VertexController {
    public static final String PATH_CONTROLLER = ApiConstants.API_LATEST_ROUTE + "vertex";

    private final VertexService vertexService;

    public VertexController(VertexService vertexService) {
        this.vertexService = vertexService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ApiReplyAO<?> get() {
        return ApiReplyAOFactory.ok(vertexService.schema());
    }
}
