package u.pankratova;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class VoteStatus {
  private String topicName;
  private String voteName;
  private String description;
  private List<String> options = new ArrayList<>();
  private int numberOptions;
  private String creator;

  private boolean isVoting;

  public void addOption(String option){
    options.add(option);
  }
}
