package u.pankratova;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientHandler extends ChannelInboundHandlerAdapter {
  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    System.out.println("Someone connected...");
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    System.out.println(msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    System.out.println("\nThe connection was closed because the server was shut down");
    System.exit(0);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
