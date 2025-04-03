package u.pankratova;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class Vote {
  private String name;
  private String description;
  private int numberAnswers;
  private List<String> possibleAnswers;
  private String creatorVote;
  private Map<String, Integer> votingResult;

  public Vote(String name, String description, int numberOptions, List<String> options, String creator) {
    this.name = name;
    this.description = description;
    this.possibleAnswers = options;
    this.creatorVote = creator;
    this.votingResult = new HashMap<>();

    for (String option : options) {
      votingResult.put(option, 0);
    }
  }

  public void vote(String option) {
    votingResult.put(option, votingResult.getOrDefault(option, 0) + 1);
  }
}
