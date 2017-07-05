package org.catinthedark.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import org.catinthedark.shared.serialization.Deserializer
import org.catinthedark.shared.serialization.NettyDecoder
import org.catinthedark.shared.serialization.NettyEncoder
import org.catinthedark.shared.serialization.Serializer

class TCPClient(
        private val serializer: Serializer,
        private val deserializer: Deserializer
) {
    private val group = NioEventLoopGroup()
    private val bootstrap = Bootstrap()

    init {
        bootstrap.group(group)
                .channel(NioSocketChannel::class.java)
                .handler(object : ChannelInitializer<AbstractChannel>() {
                    override fun initChannel(ch: AbstractChannel) {
                        val pipe = ch.pipeline()

                        pipe.addLast("decoder", NettyDecoder(deserializer))
                        pipe.addLast("encoder", NettyEncoder(serializer))
                    }
                })
                .option(ChannelOption.SO_KEEPALIVE, true)
    }

    fun connect(host: String, port: Int) {
        bootstrap.connect(host, port).addListener(object : ChannelFutureListener {
            override fun operationComplete(future: ChannelFuture) {
                if (future.isSuccess) {
                    println("CONNECTED")
                    addCloseDetectListener(future.channel())
                    send(future.channel())
                } else {
                    println("FAIL: ${future.cause()}")
                    future.channel().close()
                    bootstrap.connect(host, port).addListener(this) // reconnect
                }
            }

            private fun addCloseDetectListener(channel: Channel) {
                channel.closeFuture().addListener(object : ChannelFutureListener {
                    override fun operationComplete(future: ChannelFuture?) {
                        println("DISCONNECT")
                    }
                })
            }

            private fun send(channel: Channel) {
                if (channel.isActive) {
                    println("Send message")
                    channel.write("Hello World")
                }
            }
        })
    }
}