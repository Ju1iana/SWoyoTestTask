package u.pankratova;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import java.util.Scanner;
import io.netty.channel.socket.SocketChannel;

public class VotingClient {
  private static final String HOST = "localhost";
  private static final int PORT = 8080;

  public static void main(String[] args) throws Exception {
    EventLoopGroup group = new NioEventLoopGroup();

    try {
      Bootstrap b = new Bootstrap();
      b.group(group)
        .channel(NioSocketChannel.class)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(new StringDecoder(), new StringEncoder(), new ClientHandler());
          }
        });

      ChannelFuture f = b.connect(HOST, PORT).sync();
      Channel channel = f.channel();

      Scanner scanner = new Scanner(System.in);
      while (true) {
        String input = scanner.nextLine();
        channel.writeAndFlush(input + "\n");
      }
    } finally {
      group.shutdownGracefully();
    }
  }
}
