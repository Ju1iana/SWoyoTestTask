package u.pankratova;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.List;
import java.util.Map;

public class ServerHandler extends SimpleChannelInboundHandler<String> {
  public User currentUser;
  public Vote currentVote;
  private VoteStatus vs;

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("Someone connected...\n");
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, String msg) {

    // Если идет процесс создания голосования - обрабатываем сообщение как часть процесса
    if (vs != null && (vs.isVoting() || vs.getVoteName() == null)) {
      createNewVote(ctx, msg.trim());
      return;
    }

    if (vs != null) {
      if (vs.isVoting()) {
        selectVote(ctx, new String[]{msg.trim()});
      } else {
        createNewVote(ctx, msg);
      }
      return;
    }

    if (currentVote != null) {
      activeVote(ctx, msg);
      return;
    }

    String[] elements = msg.split("\\s+");
    if (elements.length == 0) {
      ctx.writeAndFlush("Enter the command\n");
    }

    String command = elements[0];

    switch (command) {
      case "login":
        if (elements.length < 2) {
          ctx.writeAndFlush("Error: use 'login -u=username'\n");
        } else {
          loginAsUsername(ctx, elements[1]);
        }
        break;

      case "create":
        if (elements.length < 3)
          ctx.writeAndFlush("Error in creation'\n");
        else
          createTopic(ctx, elements);
        break;

      case "view":
        if (elements.length < 3) {
          viewAllTopics(ctx, VotingServer.getTopics()); // Если без аргументов - показываем все топики
        } else {
          String topicName = elements[1].substring(3); // -t=<topic>
          String voteName = elements[2].substring(3);  // -v=<vote>
          viewAllInformation(ctx, topicName, voteName);
        }
        break;

      case "vote":
        if (elements.length < 3)
          ctx.writeAndFlush("Error: use 'vote -t=<topic> -v=<vote>'\n");
        else
          selectVote(ctx, elements);
        break;

      case "delete":
        if (elements.length < 3)
          ctx.writeAndFlush("Error: use 'delete -t=<topic> -v=<vote>'\n");
        else {
          deleteVote(ctx, elements);
        }
        break;

      case "exit":
        ctx.writeAndFlush("The exit has been realized");
        ctx.close();
        break;

      default:
        ctx.writeAndFlush("Unknown command\n");
    }
  }

  public void loginAsUsername(ChannelHandlerContext ctx, String str) {
    // Проверяем, не залогинен ли пользователь уже
    if (currentUser != null) {
      ctx.writeAndFlush("Error: You are already logged in as " + currentUser.getUsername() + "\n");
      return;
    }

    if (!str.startsWith("-u=") || str.substring(3).equals("")) {
      ctx.writeAndFlush("Error: Use 'login -u=username'\n");
      return;
    }

    String username = str.substring(3);

   /* // Проверяем, не занято ли имя
    if (VotingServer.getUsers().containsKey(username)) {
      ctx.writeAndFlush("Error: Username '" + username + "' is already taken\n");
      return;
    } */

    // Если всё успешно
    currentUser = new User(username);
    VotingServer.getUsers().put(username, currentUser);
    ctx.writeAndFlush("Logged in as " + username + "\n");
  }

  public void createTopic(ChannelHandlerContext ctx, String[] elements) {
    if (currentUser == null) {
      ctx.writeAndFlush("You need to login first\n");
      return;
    }

    if (elements.length < 3) {
      ctx.writeAndFlush("Error in writing the command\n");
      return;
    }

    String topicName = elements[2].substring(3);

    if (elements[1].equals("topic")) {
      if (!elements[2].startsWith("-n=")) {
        ctx.writeAndFlush("Error: Use create topic -n=<topic>\n");
        return;
      }
      if (VotingServer.getTopics().containsKey(topicName)) {
        ctx.writeAndFlush("Error: Topic '" + topicName + "' already exists\n");
      } else {
        VotingServer.getTopics().put(topicName, new Topic(topicName));
        ctx.writeAndFlush("Topic " + topicName + " created\n");
      }
    } else if (elements[1].equals("vote")) {

      // Cоздали новое голосование в выбранном топике
      Topic topic = VotingServer.getTopics().get(topicName);

      if (topic == null) {
        ctx.writeAndFlush("Topic " + topicName + " does not exist\n");
        return;
      }

      this.vs = new VoteStatus();
      vs.setVoting(true);
      vs.setTopicName(topicName);

      ctx.writeAndFlush("Enter a unique voting name: ");
    } else {
      ctx.writeAndFlush("Error in creation. Try again\n");
    }
  }

  public void viewAllTopics(ChannelHandlerContext ctx, Map<String, Topic> topics) {
    if (topics.isEmpty()) {
      ctx.writeAndFlush("No topics found\n");
      return;
    }

    StringBuilder output = new StringBuilder("Topics: (" + topics.size() + ")\n");

    for (String name : topics.keySet()) {
      output.append(name).append(":\n");

      for (Vote vote : topics.get(name).getVoteList()) {
        output.append("- ").append(vote.getName()).append(" | by ").append(vote.getCreatorVote()).append("\n");
      }
    }
    ctx.writeAndFlush(output.toString());
  }

  public void viewAllInformation(ChannelHandlerContext ctx, String topicName, String voteName) {
    Topic topic = VotingServer.getTopics().get(topicName);
    if (topic == null) {
      ctx.writeAndFlush("Topic not found: " + topicName + "\n");
      return;
    }

    Vote vote = topic.getVoteList().stream()
      .filter(v -> v.getName().equals(voteName))
      .findFirst()
      .orElse(null);

    if (vote == null) {
      ctx.writeAndFlush("Vote not found: " + voteName + "\n");
      return;
    }

    StringBuilder response = new StringBuilder();
    response.append("Topic: ").append(topicName).append("\n");
    response.append("Vote: ").append(voteName).append("\n");
    response.append("Description: ").append(vote.getDescription()).append("\n");
    response.append("Results:\n");

    for (String option : vote.getPossibleAnswers()) {
      int votes = vote.getVotingResult().getOrDefault(option, 0);
      response.append("- ").append(option)
        .append(": ").append(votes).append(" votes\n");
    }
    ctx.writeAndFlush(response.toString());
  }

  public void createNewVote(ChannelHandlerContext ctx, String msg) {

    if (vs.getVoteName() == null) {
      Topic topic = VotingServer.getTopics().get(vs.getTopicName());

      boolean exists = topic.getVoteList().stream()
        .anyMatch(v -> v.getName().equals(msg.trim()));

      if (exists) {
        ctx.writeAndFlush("Error: Vote '" + msg.trim() + "' already exists! Try another name: ");
        return;
      }
    }

    if (vs.getVoteName() == null) {
      vs.setVoteName(msg);
      ctx.writeAndFlush("Enter vote description: ");
      return;

    } else if (vs.getDescription() == null) {
      vs.setDescription(msg);
      ctx.writeAndFlush("Enter a number of possible answers: ");
      return;

    } else if (vs.getNumberOptions() == 0) {
      int options = Integer.parseInt(msg.trim());

      if (options <= 0) {
        ctx.writeAndFlush("Error a number of possible answers\n");
        return;
      }

      vs.setNumberOptions(options);
      ctx.writeAndFlush("Enter option 1: ");
      return;
    }

    if (vs.getOptions().size() < vs.getNumberOptions()) {
      vs.addOption(msg.trim());

      if (vs.getOptions().size() < vs.getNumberOptions()) {
        ctx.writeAndFlush("Enter option " + (vs.getOptions().size() + 1) + ": ");
      } else {
        Topic topic = VotingServer.getTopics().get(vs.getTopicName());
        Vote vote = new Vote(vs.getVoteName(),
          vs.getDescription(),
          vs.getNumberOptions(),
          vs.getOptions(),
          currentUser.getUsername());

        topic.addVote(vote);
        ctx.writeAndFlush("Vote '" + vote.getName() + "' created successfully with "
          + vs.getNumberOptions() + " options!\n");
        vs = null;
      }
    }
  }

  private void selectVote(ChannelHandlerContext ctx, String[] elements) {
    try {
      String topicName = elements[1].substring(3);
      String voteName = elements[2].substring(3);

      Topic topic = VotingServer.getTopics().get(topicName);
      if (topic == null) {
        ctx.writeAndFlush("Topic not found: " + topicName + "\n");
        return;
      }

      Vote vote = topic.getVoteList().stream()
        .filter(v -> v.getName().equals(voteName))
        .findFirst()
        .orElse(null);

      if (vote == null) {
        ctx.writeAndFlush("Vote not found: " + voteName + "\n");
        return;
      }

      // Показываем всю информацию и сохраняем состояние
      if (elements.length == 3) {
        viewVote(ctx, topicName, voteName);
        currentVote = vote;
        return;
      }

    } catch (NumberFormatException e) {
      ctx.writeAndFlush("Error: Choice must be a number\n");
    }
  }

  // Если есть активное голосование
  private void activeVote(ChannelHandlerContext ctx, String msg) {
    try {
      int choice = Integer.parseInt(msg.trim()) - 1;
      List<String> options = currentVote.getPossibleAnswers();

      if (choice < 0 || choice >= options.size()) {
        ctx.writeAndFlush("Invalid choice. Try again (1-" + options.size() + "): ");
        return;
      }

      currentVote.vote(options.get(choice));
      ctx.writeAndFlush("Voted for: " + options.get(choice) + "\n");
      currentVote = null;

    } catch (NumberFormatException e) {
      ctx.writeAndFlush("Please enter a number: ");
    }
    return;
  }

  private void viewVote(ChannelHandlerContext ctx, String topicName, String voteName) {
    Topic topic = VotingServer.getTopics().get(topicName);
    if (topic == null) {
      ctx.writeAndFlush("Topic '" + topicName + "' not found\n");
      return;
    }

    Vote vote = topic.getVoteList().stream()
      .filter(v -> v.getName().equals(voteName))
      .findFirst()
      .orElse(null);

    if (vote == null) {
      ctx.writeAndFlush("Vote '" + voteName + "' not found\n");
      return;
    }

    StringBuilder options = new StringBuilder("Voting options:\n");
    for (int i = 0; i < vote.getPossibleAnswers().size(); i++) {
      options.append(i + 1).append(". ").append(vote.getPossibleAnswers().get(i)).append("\n");
    }
    options.append("Enter your choice (1-").append(vote.getPossibleAnswers().size()).append("): ");
    ctx.writeAndFlush(options.toString());
  }

  public void deleteVote(ChannelHandlerContext ctx, String[] elements) {
    String topicName = elements[1].substring(3);
    String voteName = elements[2].substring(3);

    Topic topic = VotingServer.getTopics().get(topicName);
    if (topic == null) {
      ctx.writeAndFlush("Topic not found: " + topicName + "\n");
      return;
    }

    Vote vote = topic.getVoteList().stream()
      .filter(v -> v.getName().equals(voteName))
      .findFirst()
      .orElse(null);

    if (vote == null) {
      ctx.writeAndFlush("Vote not found: " + voteName + "\n");
      return;
    }

    if (currentUser.getUsername().equals(vote.getCreatorVote())) {
      topic.removeVote(vote);
      ctx.writeAndFlush("Vote deleted: " + voteName + "\n");
    } else {
      ctx.writeAndFlush("You are not the creator of this vote\n");
    }
  }
}
