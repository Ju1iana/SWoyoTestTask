package u.pankratova;

import java.util.ArrayList;
import java.util.List;

public class Topic {
  private String name;
  private List<Vote> voteList;

  public Topic(String name) {
    this.name = name;
    this.voteList = new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public List<Vote> getVoteList() {
    return voteList;
  }

  public void addVote(Vote vote){
    voteList.add(vote);
  }

  public void removeVote(Vote vote) {
    voteList.remove(vote);
  }
}
