package gg.rsmod.plugins.protocol.codec.game

import com.github.michaelbull.logging.InlineLogger
import gg.rsmod.game.message.PacketLength
import gg.rsmod.game.message.ServerPacket
import gg.rsmod.game.message.ServerPacketStructureMap
import gg.rsmod.util.IsaacRandom
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

private val logger = InlineLogger()

class GameSessionEncoder(
    private val isaacRandom: IsaacRandom,
    private val structures: ServerPacketStructureMap
) : MessageToByteEncoder<ServerPacket>() {

    override fun encode(
        ctx: ChannelHandlerContext,
        msg: ServerPacket,
        out: ByteBuf
    ) {
        val structure = structures[msg]
        if (structure == null) {
            logger.error { "Structure for packet not defined (packet=$msg)" }
            return
        }

        val opcode = (structure.opcode + isaacRandom.opcodeModifier) and 0xFF
        val length = when (structure.length) {
            PacketLength.Short -> 0xFFFF
            else -> 0xFF
        }

        val buf = ctx.alloc().buffer(length)

        structure.write(msg, buf)

        out.writeByte(opcode)
        if (structure.length == PacketLength.Byte) {
            out.writeByte(buf.writerIndex())
        } else if (structure.length == PacketLength.Short) {
            out.writeShort(buf.writerIndex())
        }
        out.writeBytes(buf)
    }
}
