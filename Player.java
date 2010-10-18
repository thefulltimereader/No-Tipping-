import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class Player {

  public static void main(String args[]){
    Player p = new Player("test");
    //String fromS = "ADD|3,-4|in=-6.0,out=-6.0";
    String fromS = "REMOVE|9,-14 7,-10 5,-9 1,-8 3,-7 10,-6 5,-5 3,-4 1,-3 3,-2 6,-1 7,6 10,7 9,8|in=-116.0,out=-48.0";
    System.out.println("Server: " + fromS);
    p.setStatus(fromS);
    //p._usedWeights.add(1);p._usedWeights.add(2);
    //p._usedWeights.add(4);p._usedWeights.add(6);
    //p._usedWeights.add(5);p._usedWeights.add(7);p._usedWeights.add(8);
    //p._usedWeights.add(10);
    System.out.println("Final result:  " + p.play());    
  }
  private int _mode; // if mode = 0, its REMOVE, if 1 its ADD
  //stores key = position, value = weight of occupied positions 
  private HashMap<Integer, Integer> _occupied = new HashMap<Integer, Integer>();
  private int _numOfWeights;
  private Collection<Integer> _usedWeights = new ArrayList<Integer>();
  public Player(String name){
    _numOfWeights = 10;
  }
  private int _plays = 0;

  public String play(){
    System.out.println("+++++++++++++My " + _plays+ "th play +++++++++++++");
    //ADD
    ChoicePair finalResult = null;
    Random gen = new Random();
    if(_mode==1){
      List<ChoicePair> poss = buildChoices(_occupied, true, false);
      if(poss.isEmpty()) System.out.println(poss+"no more..");
      System.out.println("usedWeights:"+_usedWeights);
      if(poss.isEmpty() || _occupied.size() == 29){
        List<ChoicePair> choices = buildChoices(_occupied, true, true);//this will tip..
        if(!choices.isEmpty()){
          int r = gen.nextInt(choices.size());//get random
          finalResult = choices.get(r);
        }
        else finalResult = new ChoicePair(-1,-1, _occupied); //invalid shouldn't reach here
        //will get here if playing alone
      }
      else if(poss.size()==1) finalResult = poss.get(0);
      else{
        Pair<Double, ChoicePair> res = alphaBeta(_occupied, poss, 
            new Pair<Double, ChoicePair>(Double.NEGATIVE_INFINITY, poss.get(0)), 
            new Pair<Double, ChoicePair>(Double.POSITIVE_INFINITY, poss.get(0)), 
            null, 0);
        finalResult = res.snd;
      }
    }
    //remove!!
    else{
      List<ChoicePair> poss = buildRemoveChoices(_occupied,false);
      System.out.println("remove possibilities: " + poss);
      System.out.println("occupied are: " + _occupied);
      if(poss.isEmpty() || _occupied.size()==1){
        
        List<ChoicePair> choices = buildRemoveChoices(_occupied, true);//this will tip..
        if(!choices.isEmpty()){
          int r = gen.nextInt(choices.size());//get random
          finalResult = choices.get(r);
        }
        else finalResult = new ChoicePair(-100,-100, _occupied);//invalid. should not reach
      }
      else if(poss.size()==1) finalResult = poss.get(0);
      else{
        Pair<Double, ChoicePair> res = alphaBetaRemove(_occupied, poss, 
            new Pair<Double, ChoicePair>(Double.NEGATIVE_INFINITY, poss.get(0)), 
            new Pair<Double, ChoicePair>(Double.POSITIVE_INFINITY, poss.get(0)), 
            null, 0);
        finalResult = res.snd;
      }
    }
    System.out.println("use weight!!! "+finalResult.weight+ " at pos:" + finalResult.position);
    System.out.println("My torque is left:"+finalResult.leftT+ " right:" + finalResult.rightT);
    if(_mode == 1 && finalResult.weight>0)_usedWeights.add(finalResult.weight);
    _plays++;
    return finalResult.toString();
  }
  /**parses details sent from Server and sets Game Status
   * @fromServer looks like "ADD|3,-4|in=-9.0,out=3.0
   */
  public void setStatus(String fromServer){
    Pattern splitter = Pattern.compile("\\|");
    String[] words = splitter.split(fromServer);
    if(words[0].startsWith("ADD")) _mode = 1;
    else{
      _mode = 0;
      _occupied.clear();
    }
    String data = words[1];
    StringTokenizer parser = new StringTokenizer(data, " "); 
    //feed the occupied map
    while(parser.hasMoreTokens()){
      String[] token = parser.nextToken().split(",");
      _occupied.put(Integer.valueOf(token[1]), Integer.valueOf(token[0]));
    }

    //printStatus();

  }
  private void printStatus(){
    System.out.println("Current playing mode is " + _mode);
    System.out.println("Current position occupied are: " + _occupied.toString());
  }
  private List<ChoicePair>buildChoices(HashMap<Integer, Integer> occupied,
      boolean first, boolean losing){
    Set<Integer> takenPositions = occupied.keySet();   
    Collection<Integer> usedW = occupied.values();
    ArrayList<ChoicePair> possibilities = new ArrayList<ChoicePair>();
    //System.out.println("occupied Positions:" + occupied + "taken Positions:" + takenPositions);
    for(int i=-15; i<16;i++){
      if(!takenPositions.contains(i)){
        for(int j=1; j<=_numOfWeights;j++){
          //if first is on, it is called in the beginning and so should not consider what's in occupied values
          if(!_usedWeights.contains(j) && (first || !usedW.contains(j) )){
            //the second one hsould be occupied not _occupied but what the hell, search sp too big
            ChoicePair choice = first ? new ChoicePair(i,j, _occupied): new ChoicePair(i, j, occupied);
            //is losing is on, need to add everything regardless
            if(losing) System.out.println("Losing..");//System.out.println("Honmani akan no?" + new ChoicePair(i,j).willTipWith(null));
            if(losing || !choice.willTipWith() ) possibilities.add(choice);
          }
        }
      }
    }
    return possibilities;
  }

  private List<ChoicePair>buildRemoveChoices(HashMap<Integer, Integer> occupied,
      boolean losing){
    Set<Integer> takenPositions = occupied.keySet();   
    ArrayList<ChoicePair> possibilities = new ArrayList<ChoicePair>(); //System.out.println("occupied Positions:" + occupied + "taken Positions:" + takenPositions);
    for(Integer pos : takenPositions){
       HashMap<Integer, Integer> removedOccupied = new HashMap<Integer, Integer>(occupied);
       removedOccupied.remove(pos);
       ChoicePair choice = new ChoicePair(pos, occupied.get(pos), removedOccupied);
       if(losing || !choice.willTipWith()) possibilities.add(choice);            
    }
    return possibilities;
  }



  /**
   * Calculate heuristic scores for each occupied
   * Kill huerisitcs to stop...
   * Scores are based on as far as you can go within certain time limits
   * The number of options you leave to the opponent
   * The number of options that leaves you in the next level
   * The heavier the weight, the worse,,
   * the smaller the choice you produce... better???
   * best if you eliminate the search space faster...
   * @param position
   * @param weight
   * @param choices of positions and weights
   * @return score
   */
  private Pair<Double, ChoicePair> alphaBeta(HashMap<Integer, Integer> curr, List<ChoicePair> possibilities, Pair<Double, ChoicePair> alpha, 
      Pair<Double, ChoicePair> beta, ChoicePair node, double depth){
    depth++;
    if(depth==4){
      Pair<Double, ChoicePair> best = new Pair<Double,ChoicePair>(Double.MIN_VALUE, node);
      for(ChoicePair p: possibilities){
        double score = 1/(depth*node.weight);//+node.position;
        best = (score>best.fst)? new Pair<Double, ChoicePair>(score, p): best;
      }
      return best;
    }
    //this is good..
    //if(possibilities.size()==0) return new Pair<Double, ChoicePair>(1.0, node);
    if(possibilities.size()==0){
      //invert the depth = depth smaller the better -> smaller depths should give high score
      //plus heavier the better
      //the smaller the resulting possibilities,, better
      //double score = 1/(depth) + node.weight + Math.abs(node.position);
      double score = 1/(depth*node.weight);//Math.abs(node.position);
      return new Pair<Double, ChoicePair>(score, node);
    }
    for(ChoicePair choice: possibilities){
      HashMap<Integer, Integer> newOccupied = new HashMap<Integer, Integer>(curr);
      newOccupied.put(choice.position, choice.weight);
      double score;
      List<ChoicePair> newPossibilities = buildChoices(newOccupied, false, false);
      //if occupied has all 9 weights used already, go to scoring (9 weights+orignal) = 10
      if(newOccupied.values().size()==10){
        score = 1/(depth*newPossibilities.size());
      }
      else{
      //System.out.println("for choice: " +choice+" new Occupied: " +newOccupied);
      //System.out.println("for choice:" + choice+" poss:"+newPossibilities);
      Pair<Double, ChoicePair> newAlpha = new Pair<Double, ChoicePair>(alpha.fst*-1, alpha.snd);//negate
      Pair<Double, ChoicePair> newBeta = new Pair<Double, ChoicePair>(beta.fst*-1, beta.snd);//negat
      Pair<Double, ChoicePair>result = alphaBeta(newOccupied, newPossibilities, newBeta, newAlpha, choice, depth);
      score = -1*result.fst; 
      }
      //max(alpha, -alphabeta(...))
      if(score>alpha.fst){
        alpha = new Pair<Double, ChoicePair>(score, choice); //keep the winner
      }
      if (beta.fst<= alpha.fst){/*System.out.println("Don't give a fuck!!");*/break;} 
    }
    return alpha;
  }
  private Pair<Double, ChoicePair> alphaBetaRemove(HashMap<Integer, Integer> curr, List<ChoicePair> possibilities, Pair<Double, ChoicePair> alpha, 
      Pair<Double, ChoicePair> beta, ChoicePair node, double depth){
    depth++;
    //System.out.println("==============Depths is "+depth);
    if(depth>7){
      //...to big..
      Pair<Double, ChoicePair> best = new Pair<Double,ChoicePair>(Double.MIN_VALUE, node);
      for(ChoicePair p: possibilities){
        double score = 1/(depth*10)+node.weight+node.position;
        best = (score>best.fst)? new Pair<Double, ChoicePair>(score, p): best;
      }
      return best;
    }
    if(possibilities.size()==0){ //TODO: rethink for remove
      //invert the depth = depth smaller the better -> small should give high score
      //plus heavier the better the smaller the resulting possibilities,, better
      double score = 1/(depth*10)+node.weight+node.position;
      //System.out.println("Score is: " + score + " with depth*node.w" + depth*node.weight);
      return new Pair<Double, ChoicePair>(score, node);
    }
    for(ChoicePair choice: possibilities){
      HashMap<Integer, Integer> newOccupied = new HashMap<Integer, Integer>(curr);
      newOccupied.remove(choice.position);
      //System.out.println("for choice: " +choice+" new Occupied: " +newOccupied);
      List<ChoicePair> newPossibilities = buildRemoveChoices(newOccupied,false);
      //System.out.println("for choice:" + choice+" poss:"+newPossibilities);
      Pair<Double, ChoicePair> newAlpha = new Pair<Double, ChoicePair>(alpha.fst*-1, alpha.snd);//negate
      //alpha = newAlpha;
      Pair<Double, ChoicePair> newBeta = new Pair<Double, ChoicePair>(beta.fst*-1, beta.snd);//negate
      //beta = newBeta;
      Pair<Double, ChoicePair>result = alphaBetaRemove(newOccupied, newPossibilities, newBeta, newAlpha, choice, depth);
      double score = -1*result.fst; 
  //    System.out.println("for choice:" + choice+"a"+alpha.fst+" score:" + score+" beta:" + beta.fst);
      //max(alpha, -alphabeta(...)
      if(score>alpha.fst){
        alpha = new Pair<Double, ChoicePair>(score, choice); //keep the winner
      }
      if (beta.fst<= alpha.fst){/*System.out.println("Don't give a fuck!!");*/break;} 
    }
    //System.out.println("kore kaesou" + alpha.fst);
    return alpha;
  }


  class ChoicePair{
    private int position;
    double rightT,leftT = 0.0; 
    private int weight;
    private HashMap<Integer, Integer> myOccupied;
    ChoicePair(int position, int weight, Map<Integer, Integer> occ){
      this.position = position;
      this.weight = weight;
      this.myOccupied = new HashMap<Integer, Integer>(occ);
      if(_mode==1) myOccupied.put(position, weight);
    }
    boolean willTipWith(){
      //if(newOccupied==null) newOccupied = myOccupied;
      rightT = leftT = 0.0;
      double in1=0,out1=0,in3=0,out3=0;
      in3 += 9;
      in1 += 3;

      Set<Integer> s = myOccupied.keySet();
      for (Integer i: s){
        int pos = i;
        int wt;
        wt = myOccupied.get(pos); 
        if (pos < -3)
          out3 += (-1) * (pos-(-3)) * wt;
        else
          in3 += (pos-(-3))* wt;
        if (pos < -1)
          out1 += (-1) * (pos-(-1)) * wt;
        else
          in1 += (pos-(-1))* wt;
      }
      rightT= in1 - out1;
      leftT = out3 - in3;
      if(rightT>0 || leftT>0) return true;
      return false;
    }
    @Override
    public String toString() {
      return weight+"," + position;
    }
  }
  class Pair<T, S>{
    private T fst;
    private S snd;
    public Pair(T fst, S snd){
      this.fst = fst;
      this.snd = snd;
    }
  }
}
