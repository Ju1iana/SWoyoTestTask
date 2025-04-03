import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import u.pankratova.*;

import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
public class ServerHandlerTest {

  @Mock
  private ChannelHandlerContext ctx;

  private ServerHandler serverHandler;

  @BeforeEach
  public void setUp() {
    serverHandler = new ServerHandler();
    VotingServer.getUsers().clear();
    VotingServer.getTopics().clear();
  }

  public User createNewTestUser() {
    return serverHandler.currentUser = new User("testUser");
  }


  // login:

  @Test
  public void enterUsernameShouldBeSuccess() {
    // Arrange
    String username = "login -u=testUser";

    // Act
    serverHandler.channelRead0(ctx, username);

    // Assert
    verify(ctx).writeAndFlush("Logged in as testUser\n");
  }

  @Test
  public void usernameIsAlreadyLogged() {
    // Arrange
    String command = "login -u=testUser";
    VotingServer.getUsers().put("testUser", createNewTestUser());

    // Act
    serverHandler.channelRead0(ctx, command);

    // Assert
    verify(ctx).writeAndFlush("Error: You are already logged in as " +
      VotingServer.getUsers().get("testUser").getUsername() + "\n");
  }

  @ParameterizedTest
  @ValueSource(strings = {"-u=", "u=Anna", "login", "", " -u=Anna"})
  public void wrongFormatUsername(String invalidInput) {
    // Act
    serverHandler.loginAsUsername(ctx, invalidInput);

    // Assert
    verify(ctx).writeAndFlush("Error: Use 'login -u=username'\n");
  }

  // topic:

  @Test
  public void createTopicShouldBeSuccess() {
    // Arrange
    createNewTestUser();
    String command = "create topic -n=testTopic";

    // Act
    serverHandler.channelRead0(ctx, command);

    // Asser
    verify(ctx).writeAndFlush("Topic testTopic created\n");
  }

  @Test
  public void topicAlreadyExists() {
    // Arrange
    String command = "create topic -n=testTopic";
    VotingServer.getTopics().put("testTopic", new Topic("testTopic"));
    serverHandler.currentUser = createNewTestUser();

    // Act
    serverHandler.createTopic(ctx, command.split("\\s+"));

    // Assert
    verify(ctx).writeAndFlush("Error: Topic '" +
      VotingServer.getTopics().get("testTopic").getName() + "' already exists\n");
  }

  @ParameterizedTest
  @ValueSource(strings = {"-n=", "n=testTopic", "create", "", " -n=testTopic"})
  public void wrongFormatTopicName(String invalidInput) {
    // Arrange
    createNewTestUser();

    // Act
    serverHandler.createTopic(ctx, invalidInput.split("\\s+"));

    // Assert
    verify(ctx).writeAndFlush("Error in writing the command\n");
  }


  @Test
  public void shouldNoTopicsFound() {
    // Arrange
    Map<String, Topic> topics = Collections.emptyMap();

    // Act
    serverHandler.viewAllTopics(ctx, topics);

    // Assert
    verify(ctx).writeAndFlush("No topics found\n");
  }

  @Test
  void viewTopicWithoutVotes() {
    // Arrange
    Map<String, Topic> topics = new HashMap<>();
    topics.put("Books", new Topic("Books"));

    // Act
    serverHandler.viewAllTopics(ctx, topics);

    // Assert
    verify(ctx).writeAndFlush("Topics: (1)\nBooks:\n");
  }

  @Test
  void viewMultipleTopics() {
    // Arrange
    Topic topic1 = new Topic("Topic1");
    topic1.addVote(new Vote("topicName1", "", 1, List.of(), "username1"));

    Topic topic2 = new Topic("Topic2");
    topic2.addVote(new Vote("topicName2", "", 2, List.of(), "username2"));

    Map<String, Topic> orderedMap = new LinkedHashMap<>();
    orderedMap.put("Topic1", topic1);
    orderedMap.put("Topic2", topic2);

    // Act
    serverHandler.viewAllTopics(ctx, orderedMap);

    // Assert
    verify(ctx).writeAndFlush(
      "Topics: (2)\nTopic1:\n- topicName1 | by username1\nTopic2:\n- topicName2 | by username2\n"
    );
  }
}
