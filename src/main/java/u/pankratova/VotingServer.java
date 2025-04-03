package u.pankratova;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class VotingServer {
  private static final int PORT = 8080;
  private static Map<String, Topic> topics = new HashMap<>();
  private static Map<String, User> users = new HashMap<>();

  public static void main(String[] args) {

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(new StringDecoder(), new StringEncoder(), new ServerHandler());
          }
        });

      consoleListener(bossGroup, workerGroup);

      ChannelFuture future = b.bind(PORT).sync();
      System.out.println("Server started on port " + PORT);
      future.channel().closeFuture().sync();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  public static Map<String, Topic> getTopics() {
    return topics;
  }

  public static Map<String, User> getUsers() {
    return users;
  }

  private static void consoleListener(EventLoopGroup b, EventLoopGroup w) {
      new Thread(() -> {
        Scanner scanner = new Scanner(System.in);
        while (true) {
          String input = scanner.nextLine();
          if ("exit".equalsIgnoreCase(input.trim())) {
            System.out.println("Shutting down server...");
            b.shutdownGracefully();
            w.shutdownGracefully();
            break;
          }
        }
      }).start();
    }
}
