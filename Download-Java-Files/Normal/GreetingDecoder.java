package com.criteo.hadoop.garmadon.forwarder.handler;

import com.criteo.hadoop.garmadon.protocol.ProtocolConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

public class GreetingDecoder extends ReplayingDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        out.add(buf.readBytes(ProtocolConstants.GREETING_SIZE));
        ctx.pipeline().remove(this);
    }
}
