package featureExtraction;

public class Word {
     // This class represents the words foudn in the sentiwordnet file
     // they encapsulate the type of word, pos score, neg score, obj score and instance of the word also
     String word;
     char type;
     float pos;
     float neg;
     float obj;
     int instance;
     public Word() {
         pos = 0.0f;
         neg = 0.0f;
         obj = 0.0f;
         instance = 0;
     }
     public Word(String _word, char _type, float _pos, float _neg, int _inst) {
         word = _word;
         type = _type;
         pos = _pos;
         neg = _neg;
         obj = 1.0f - (pos + neg);
         instance = _inst;
     }

     // get and set methods
     public String getWord() { return word; }
     public char getType() { return type; }
     public float getPositiveScore() { return pos; }
     public float getNegativeScore() { return neg; }
     public float getObjectiveScore() { return obj; }
     public int getWordInstance() { return instance; }

     public void setWord(String _word) { word = _word; }
     public void setType(char _type) { type = _type; }
     public void setPositiveScore(float _pos) { pos = _pos; }
     public void setNegativeScore(float _neg) { neg = _neg; }
     public void setWordInstance(int _inst) { instance = _inst; }
     public void setPosNegScore(float _pos, float _neg) {
         pos = _pos;
         neg = _neg;
         obj = 1.0f - (pos + neg);
     }

     // misc method that must be called to after setting positive and negative scores
     public void calculateObjectiveScore() {
         obj = 1.0f - (pos + neg);
     }
}