package qa.extract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;

import lcn.EntityFragmentFields;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.JiebaSegmenter.SegMode;
import com.huaban.analysis.jieba.SegToken;

import edu.stanford.nlp.util.Pair;
import fgmt.TypeFragment;
import rdf.EntityMapping;
import rdf.TypeMapping;
import utils.FileUtil;

final class MODNUM
{
	public static int prime=9999991;
}

enum WordType {
	NORMAL(0), ENTITY(1), LITERAL(2);

	WordType(int intVal) {
		this.intVal = intVal;
	}

	public static WordType fromInt(int intVal) {
		for (WordType wordType : values()) {
			if (wordType.intVal == intVal) {
				return wordType;
			}
		}
		return null;
	}

	int intVal;
}

//TODO: replace by nlp.ds.word
class Word {
	//type:0=normal word 1=entity 2=literal(string)
	String word;
	WordType type;
	int pos = 0;
	List<String> entList = null;

	Word(String w) {
		word = w;
		type = WordType.fromInt(0);
	}

	Word(String w, int i) {
		word = w;
		type = WordType.fromInt(i);
	}

	Word(String w, int i, int j) {
		word = w;
		type = WordType.fromInt(i);
		pos = j;
	}

	Word(String w, int i, int j, List<String> l) {
		word = w;
		type = WordType.fromInt(i);
		pos = j;
		entList = l;
	}
}

class Ent {
	public final int mod = MODNUM.prime;
	/**
	 * mention: used for entity recognition, aka synonym (in specific context),
	 * but note that one mention can point to different entities
	 * A popular way to build such a dictionary DE is by crawling Web pages and aggregating anchor links
	 * that point to Wikipedia entity pages.
	 * The frequency with which a mention (anchor text), m, links to a particular entity (anchor link), c,
	 * allows one to estimate the conditional probability pðcjmÞ
	 */
	public String entity_name, mention;
	public int no;  // aka frequecy, I guess
	public long hashe, hashm;

	// TODO: the following code fragment might be kind of immature
	public Ent(String load) {
		int indexOf9 = load.indexOf(9);  // 9 is tab's ascii code
		if (indexOf9 >= 0) {
			mention = load.substring(0, indexOf9);  // assign mention; left close and right open
			String tmp = load.substring(indexOf9 + 1);  // beginIndex; the remaining substring
			int t9 = tmp.indexOf(9);
			if (t9 >= 0) {
				entity_name = tmp.substring(0, t9);
				String numberStr = tmp.substring(t9 + 1);
				try {
					no = Integer.valueOf(numberStr);
				} catch (Exception e) {
					no = -1;
				}
			} else {
				entity_name = tmp;
			}
			hashe = calHash(entity_name);
		} else {  // return is -1 if not found
			mention = load;
			hashe = -1;
		}
		hashm = calHash(mention);
	}

	// unknown hash method
	public long calHash(String p) {
		long x = 0;
		if (p == null || p.length() == 0) return 0;
		for (int i = 0; i < p.length(); i++) {
			x = x * 65536 + (long) (int) p.charAt(i);
			x = x % mod;
		}
		return x;
	}

	@Override
	public int hashCode() {
		return (int) hashm;
	}

	public Ent() {
	}
}

public class EntityRecognitionCh {
	// Is the intersection of entMap and nentMap non-empty?
	public static HashMap<String, List<String>> entMap, nentMap;
	public static JiebaSegmenter segmenter = new JiebaSegmenter();

	public final static int MaxEnt = 20;

	static {  // 静态初始器：由static和{}组成，只在类装载的时候（第一次使用类的时候）执行一次，往往用来初始化静态变量。
		long t0 = System.currentTimeMillis();
		List<String> nent = FileUtil.readFile("data/pkubase/paraphrase/ccksminutf.txt");
		List<String> mention2ent = FileUtil.readFile("data/pkubase/paraphrase/pkubase-mention2ent.txt");

		entMap = new HashMap<>();
		nentMap = new HashMap<>();

		System.out.println("Mention2Ent size: " + mention2ent.size());
		for (String input : mention2ent) {
			Ent q = new Ent(input);
			if (entMap.containsKey(q.mention))
				entMap.get(q.mention).add(q.entity_name);
			else {
				List<String> l = new ArrayList<>();
				l.add(q.entity_name);
				entMap.put(q.mention, l);
			}
		}
		//
		/** mention: non-entity word; word: frequency
		 * so for this kind of Ent, its entity_name is the "frequency"
		 */
		for (String input : nent) {
			Ent q = new Ent(input);
			if (nentMap.containsKey(q.mention)) {
				nentMap.get(q.mention).add(q.entity_name);
			} else {
				List<String> l = new ArrayList<>();
				l.add(q.entity_name);
				nentMap.put(q.mention, l);
			}
		}

		long t1 = System.currentTimeMillis();
		System.out.println("Read Mention2Ent used " + (t1 - t0) + "ms");
	}
	
	public static boolean isAllNumber(String q)
	{
		boolean ret=true;
		for (int i=0;i<q.length();i++)
		{
			if (q.charAt(i)<48 || q.charAt(i)>57) return false;
		}
		return ret;
	}

	public static String longestFirst2(String Question) {
		String ret = "";
		String input = Question.replace('{', ' ').replace('}', ' ');

		int len = input.length();
		int[][] ex = new int[len + 3][];  // possible entity close right boundary
		Ent[][] entx = new Ent[len + 3][];  // corresponding entities
		for (int i = 0; i < len + 2; i++) {
			ex[i] = new int[len + 3];
		}
		for (int i = 0; i < len + 2; i++) {
			entx[i] = new Ent[len + 3];
		}
		for (int l = 1; l <= len; l++) {
			int pos = 0;
			for (int j = l - 1; j < len; j++) {
				// get substring of which length=l
				String searchstr = input.substring(j - l + 1, j + 1);
				List<String> rstlist = entMap.get(searchstr);

				if (rstlist != null && rstlist.size() > 0) {
					++pos;
					ex[l][pos] = j;
					entx[l][pos] = new Ent(searchstr);
				}
			}
			ex[l][0] = pos;  // number of entity hits
		}
		int covered[] = new int[len + 3];
		for (int l = len; l >= 1; l--) {
			for (int p = 1; p <= ex[l][0]; p++) {
				// for each position of left part that has no entity
				int flag = 1;
				for (int k = ex[l][p]; k >= ex[l][p] - l + 1; k--) {
					// k: entity close right boundary, decrease until the close left boundary
					if (covered[k] > 0) {  // already marked
						flag = 0;
					}
				}
				if (flag == 1) {  // not marked yet
					//1:占用  2:词头 4:词尾  8:其他
					int FLAG = 0;
					List<String> nlist = nentMap.get(entx[l][p].mention);
					if (nlist != null && nlist.size() > 0) {
						// also an non-entity word
						FLAG = 8;
					}
					if (isAllNumber(entx[l][p].mention)) {
						FLAG = 8;
					}
					// no matter what type of the word is, ends are marked
					covered[ex[l][p]] |= 4;
					covered[ex[l][p] - l + 1] |= 2;
					for (int k = ex[l][p]; k >= ex[l][p] - l + 1; k--) {
						covered[k] |= 1 | FLAG;
					}
				}
			}
		}

		/** consider the simplest situation: only one entity, it certainly wrap that entity
		 * 2nd: only one non-entity word, it's still unknown whether entMap and nentMap have a same element
		 * 3rd: two or more entity words, with no overlapping, all entity words will be marked
		 * 4th: two or more entity words, with overlapping, TODO: test...(other situations also)
		 */
		for (int i = 0; i < len; i++) {
			if ((covered[i] & 2) != 0 && (covered[i] & 8) == 0)
				ret = ret + "{";  // TODO: use append(...)
			ret = ret + Question.charAt(i);
			if ((covered[i] & 4) != 0 && (covered[i] & 8) == 0)
				ret = ret + "}";
		}
		//System.out.println("Longest First: "+ret);
		//System.out.println("Time: "+(t1-t0)+"ms");
		return ret;
	}
	//1->①
	public static String intToCircle(int i)
	{
		if (0>i || i>20) return null;
		String ret="";
		ret=ret+(char)(9311+i);
		return ret;
	}
	//①->1
	public static int circleToInt(String i)
	{
		int ret=i.charAt(0)-9311;
		if (0<ret&& ret<20) return ret;
		else return -1;
	}

	public static Pair<String, List<Word>> slotizeString(String s) {
		List<Word> candidateEntities = new ArrayList<>();
		String slottedSentence = "";
		int flag = 0;
		String word = "";
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '{') {
				flag = 1;
				continue;
			}
			if (s.charAt(i) == '}') {
				if (word.length() <= 2) {
					slottedSentence += word;
					word = "";
					flag = 0;
					continue;
				}
				int FLAG = -1;
				for (Word j : candidateEntities)  // any existing entity
					if (word.equals(j.word))
						FLAG = j.pos;
				if (FLAG == -1)  // not existing
				{
					flag = 0;
					candidateEntities.add(new Word(word, WordType.ENTITY.intVal, candidateEntities.size() + 1));
					word = "";
					slottedSentence += intToCircle(candidateEntities.size());
					continue;
				} else {
					flag = 0;
					word = "";
					slottedSentence += intToCircle(FLAG);
					continue;
				}
			}
			if (flag == 0) {
				slottedSentence += s.charAt(i);
			}
			if (flag == 1) {
				word = word + s.charAt(i);
			}
		}
		return new Pair<String, List<Word>>(slottedSentence, candidateEntities);
	}
	public static String recallNeglectedEntities(List<Word> slotWords, List<SegToken> slottedSentenceTokens) {
		int[] used = new int[slottedSentenceTokens.size() + 1];
		int[] howTokenReturn = new int[slottedSentenceTokens.size() + 1];
		for (int i = 0; i < slottedSentenceTokens.size(); i++) {
			howTokenReturn[i] = 0;
		}

		for (int len = 4; len >= 1; len--) {
			// if there are 10 words, i=0, 1, ..., 6, 0, 1, ..., 7, ..., 0, ..., 9
			for (int i = 0; i < slottedSentenceTokens.size() - len + 1; i++) {
				String tmp = "";
				int flag = 1;
				for (int j = i; j < i + len; j++) {
					tmp = tmp + slottedSentenceTokens.get(j).word;
					if (tmp.length() > 4) {  // chars, too long
						flag = 0;
						// System.out.println("tmp.length() > 4[" + tmp + "]" + len + "," + i + "," + j);
					}
					if (circleToInt(slottedSentenceTokens.get(j).word) >= 0) {
						// recognized in the precceding step
						flag = 0;
						// System.out.println("circleToInt[" + tmp + "]" + len + "," + i + "," + j);
					}
					if (used[j] == 1) {
						flag = 0;
						// System.out.println("used[j][" + tmp + "]" + len + "," + i + "," + j);
					}
					// System.out.println("pass[" + tmp + "]" + len + "," + i + "," + j);
				}
				if (flag == 0) {
					// System.out.println("flag == 0[" + tmp + "]" + len + "," + i);
					continue;
				}
				List<String> rstlist = entMap.get(tmp);
				List<String> nlist = nentMap.get(tmp);
				// unknown purpose, it should go back to how non-entity fragments is generated
				if (nlist != null && nlist.size() > 0) {
					for (int j = i; j < i + len; j++) {
						used[j] = 1;
					}
				}
				// unambiguous entity
				if (rstlist != null && rstlist.size() > 0 && (nlist == null || nlist.size() == 0)) {
					for (int j = i; j < i + len; j++) {
						used[j] = 1;
					}
					int pos = -1;
					for (Word k : slotWords)  // distinct entity
						if (tmp.equals(k.word)) {
							pos = k.pos;
							break;
						}
					if (pos > 0) {
						howTokenReturn[i] = pos;  // points to the slot of the fisrt same entity
						for (int j = i + 1; j < i + len; j++) {
							howTokenReturn[j] = -1;
						}
					} else {
						slotWords.add(new Word(tmp, 1, slotWords.size() + 1));
						howTokenReturn[i] = slotWords.size();
						for (int j = i + 1; j < i + len; j++) {  // when len>=2, bug will be invoked
							howTokenReturn[j] = -1;
						}
					}
				}

			}
		}
		String ret = "";
		for (int i = 0; i < slottedSentenceTokens.size(); i++) {
			if (howTokenReturn[i] == 0) {
				ret = ret + slottedSentenceTokens.get(i).word;
			}
			if (howTokenReturn[i] > 0) {
				ret = ret + intToCircle(howTokenReturn[i]);
			}
		}
		return ret;
	}
	public static String removeQueryId2(String question)
	{
		String ret = question;
		int st = question.indexOf(":");
		if(st!=-1 && st<6  && question.length()>4 && ((question.charAt(0)>='0' && question.charAt(0)<='9') ||question.charAt(0)=='q'))
		{
			ret = question.substring(st+1);
		}
		return ret;
	}

	public static String thirdprocess(String slottedString, List<Word> slotWords) {
		String temp = "", rets2 = "";
		int insyh = 0;
		int count = 0;
		List<Integer> lst = new ArrayList<>();
		String syh = "";
		for (int i = 0; i < slottedString.length(); i++) {
			if (circleToInt("" + slottedString.charAt(i)) != -1) {
				count++;
			} else {  // regard too long entity sequence as literal
				if (count >= 3) {  // more than two consecutive slots
					String newent = "";
					for (int j = i - count; j < i; j++) {
						newent += slotWords.get(circleToInt("" + slottedString.charAt(j)) - 1).word;
					}
					slotWords.add(new Word(newent, WordType.LITERAL.intVal, slotWords.size() + 1));
					temp += intToCircle(slotWords.size());  // TODO: bug of original code
				} else
					for (int j = i - count; j < i; j++) {
						temp += slottedString.charAt(j);
					}
				temp += slottedString.charAt(i);
				count = 0;
			}
		}
		for (int i = 0; i < temp.length(); i++) {
			if (temp.charAt(i) == '"' && insyh == 0 || temp.charAt(i) == '“') {
				insyh = 1;
				syh = "";
				rets2 += temp.charAt(i);
			} else if (temp.charAt(i) == '"' && insyh == 1 || temp.charAt(i) == '”') {
				insyh = 0;
				if (lst.size() >= 1) {
					String rp = "";
					for (int j = 0; j < syh.length(); j++) {
						int q = circleToInt("" + syh.charAt(j));
						if (q == -1)
							rp += syh.charAt(j);
						else {
							rp += slotWords.get(q - 1).word;
							//ret[q]="";
						}
					}
					slotWords.add(new Word(rp, 2, slotWords.size() + 1));
					rets2 += intToCircle(slotWords.size()) + temp.charAt(i);
				} else {
					rets2 += syh + temp.charAt(i);
				}
			} else if (insyh == 1) {
				if (circleToInt("" + temp.charAt(i)) != -1)
					lst.add(circleToInt("" + temp.charAt(i)));
				syh += temp.charAt(i);
			} else
				rets2 += temp.charAt(i);
		}
		return rets2;
	}

	public static Pair<String, List<Word>> parse(String input, JiebaSegmenter segmenter) {
		// the best way to understand this function is to run it and look at the printed vars
		String newinput = longestFirst2(input);  // entities are wrapped, eg: "{美国}和{新加坡}的水域率"

		Pair<String, List<Word>> d = null, r = new Pair<String, List<Word>>();
		r.second = new ArrayList<>();
		try {
			d = slotizeString(newinput);  // slotted string("①的水域率") and corresponding words(["新加坡"])
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		if (d != null) {
			//System.out.println(d.first);

			List<SegToken> q = segmenter.process(d.first, SegMode.SEARCH);
			String secondstr = "";
			for (SegToken t : q) {
				secondstr = secondstr + t.word + ",";
			}
			//System.out.println("First process: "+secondstr);

			String finalstring = "";
			String slottedString = recallNeglectedEntities(d.second, q);
			slottedString = thirdprocess(slottedString, d.second);

			List<SegToken> q2 = segmenter.process(slottedString, SegMode.SEARCH);
			for (SegToken t : q2) {
				finalstring = finalstring + t.word + ",";
				int p = circleToInt("" + t.word.charAt(0));
				if (p != -1) {
					Word ds = d.second.get(p - 1);
					r.second.add(new Word(ds.word, ds.type.intVal, ds.pos, entMap.get(ds.word)));
				} else {
					r.second.add(new Word(t.word, 0, -1));
				}
			}

			System.out.println("Result: " + finalstring);

			r.first = slottedString;

			return r;
		} else {
			return null;
		}
	}
	
	public static List<nlp.ds.Word> parseSentAndRecogEnt(String sent)
	{
		Pair<String, List<Word>> result = parse(sent, segmenter);  // Node Recognition; result.first = "①的水域率", result.second = ["新加坡", "的", "水域", "率"]
		if(result == null)
			return null;
		
		List<nlp.ds.Word> words = new ArrayList<nlp.ds.Word>();
		int position = 1;
		for(Word originalWord: result.second)
		{
			// Note: jieba postag is deprecated, so we utilize stanford parser to get postag in later. 
			nlp.ds.Word word = new nlp.ds.Word(originalWord.word, originalWord.word, null, position++);  // contexted word
			words.add(word);
			if(originalWord.type == WordType.ENTITY && originalWord.entList != null)  // is a entity
			{
				// Now just consider TYPE there in a smiple way.
				if(TypeFragment.typeShortName2IdList.containsKey(originalWord.word))
				{
					word.mayType = true;
					word.tmList.add(new TypeMapping(TypeFragment.typeShortName2IdList.get(originalWord.word).get(0), originalWord.word, 100.0));
				}
				word.mayEnt = true;
				word.emList = new ArrayList<EntityMapping>();
				double score = 100;
				for(String ent: originalWord.entList)
				{
					if(EntityFragmentFields.entityName2Id.containsKey(ent))
					{
						//TODO: consider more suitable entity score
						int eid = EntityFragmentFields.entityName2Id.get(ent);
//						String fstr = EntityFragmentFields.entityFragmentString.get(eid);
//						System.out.println(eid+"\t"+fstr);
						word.emList.add(new EntityMapping(eid, ent, score));
						score -= 10;  // score decreases one round by one round
					}
				}
			}
			else if(originalWord.type == WordType.LITERAL) {
				word.mayLiteral = true;
			}
			// TODO: consider TYPE
		}
		
		return words;
	}
	
	public static void main(String[] args) throws IOException {
		
		EntityFragmentFields.load();
		
		List<String> inputList = FileUtil.readFile("data/test/mini-ccks.txt");
		
		for(String input: inputList) 
		{
			if (input.length()<2 || input.charAt(0)!='q') continue;
			System.out.println("----------------------------------------");
			System.out.println(input);
			EntityRecognitionCh.parseSentAndRecogEnt(input);
		}

	}

}

