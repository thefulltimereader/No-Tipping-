import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class Player {
  
  private final String _name;
  private int _mode; // if mode = 0, its REMOVE, if 1 its ADD
  //stores key = position, value = weight of occupied positions 
  private HashMap<Integer, Integer> _occupied = new HashMap<Integer, Integer>();
  private Double _rightTorque;
  private Double _leftTorque;
  public Player(String name){
    _name = name;
  }
  public void setMode(int state){
    _mode = state;
  }
  
  public String play(){
    
    return "1,2";
  }
  /**parses details sent from Server and sets Game Status
   * @fromServer looks like "ADD|3,-4|in=-9.0,out=3.0
  */
  public void setStatus(String fromServer){
    Pattern splitter = Pattern.compile("\\|");
    String[] words = splitter.split(fromServer);
    if(words[0].startsWith("ADD")) _mode = 1;
    else _mode = 0;
//    System.out.println(words.length);
//    for(int i=0, n=words.length; i<n;i++ ){
//      System.out.println(words[i]);
//    }
    String data = words[1];
    StringTokenizer parser = new StringTokenizer(data, " "); 
    //feed the occupied map
    while(parser.hasMoreTokens()){
      String[] token = parser.nextToken().split(",");
      _occupied.put(Integer.valueOf(token[1]), Integer.valueOf(token[0]));
    }
    String torques = words[2];
    parser = new StringTokenizer(torques,",");
    _rightTorque =Double.valueOf(parser.nextToken().substring(3));
    _leftTorque = Double.valueOf(parser.nextToken().substring(4));
    printStatus();
    
  }
  private void printStatus(){
    System.out.println("Current playing mode is (0=REMOVE): " + _mode);
    System.out.println("Current position occupied are: " + _occupied.toString());
    System.out.println("Current torques are Right: "+
        _rightTorque + " left:"+_leftTorque);
    calculate_torque();
    System.out.println("Current calculated torques " +_rightTorque +
        " out="+_leftTorque );
  }
  /*
   * formula to calulate torque:
   * in1 and out1 calculates torque on lever at -1
   * in3 and out3 calculates torque on lever at -3
   * 
   * At -3, the tip occurs when torque on the left > torque on right or out3-in3 > 0
   * At -1, the tip occurs when torque on the right > torque on left or in1-out1 > 0
   * If either of the conditions hold true, the player loses the game
   */	
  private void calculate_torque() {		
    double in1=0,out1=0,in3=0,out3=0;
    Set<Integer> s = _occupied.keySet();
    for (Integer i: s){
      int pos = i;
      int wt;
      //System.out.println("position " + pos + " is occupied with wt" + _occupied.get(pos));
      wt = _occupied.get(pos); 
      
      
      if (pos < -3)
        out3 += (-1) * (pos-(-3)) * wt;
      else
        in3 += pos-(-3)* wt;


      if (pos < -1)
        out1 += (-1) * (pos-(-1)) * wt;
      else
        in1 += pos-(-1)* wt;			
    }

//    System.out.println("in1=" + in1);
//    System.out.println("out1=" + out1);
//    System.out.println("in3=" + in3);
//    System.out.println("out3=" + out3);
    _rightTorque= in1 - out1;
    _leftTorque = out3 - in3;
  }
  private boolean isBad(){
    if (_rightTorque > 0 || _leftTorque > 0) return true;
    return false;
  }
}
