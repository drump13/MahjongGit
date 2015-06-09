package system;

import static system.Kaze.PE;
import static system.Kaze.TON;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import system.Hai.HaiComparator;

/**
 * 1局を表すクラス。
 */
public class Kyoku {
	Map<Kaze, KyokuPlayer> kyokuPlayerMap;

	private final Field field;
	private final Map<Kaze, Player> playerMap; // プレイヤー
	private final Map<Kaze, Boolean> ippatuMap; // 一発判定フラグ
	private final List<Hai> yamahai; // 山牌を表すリスト
	private final List<Hai> wanpai; // 王牌を表すリスト
	private int tsumoSize; // 総ツモ枚数。これが70に達したら局終了
	private Kaze currentTurn; // 現在ターン
	private Hai currentTumohai; // 現在ツモ牌
	private Hai currentSutehai; // 現在捨て牌
	private boolean tyankanFlag; // 搶槓フラグ

	// 嶺上ツモフラグ
	private boolean rinsyanFlag;

	// 新ドラ枚数
	private int newDoraSize;

	// 槓回数
	private int kanSize;

	//後めくり槓フラグ
	private boolean atomekuriKanFlag;

	// 初順フラグ 鳴かれてもfalseとなる
	private boolean firstTurn;

	// 結果変数
	/**
	 * あがった人->上がり情報(役セット,基本点)
	 */
	private final Map<Player, AgariResult> agariMap;

	private final Map<Player, Boolean> tenpaiMap;

	private Random rand;
	private KyokuResult result;
	private KyokuRonAgariResult.Builder krbuilder;

	/**
	 * 局を生成するコンストラクタ
	 * 
	 * @param rule 採用するルール
	 * @param player 風->プレイヤーを表すマップ
	 * @param bakaze 場風
	 */
	public Kyoku(Rule rule, Map<Kaze, Player> player, Kaze bakaze) {
		this.field = new Field(rule, bakaze);
		this.kyokuPlayerMap = new HashMap<Kaze, KyokuPlayer>(4);

		this.agariMap = new HashMap<Player, AgariResult>(4);
		this.tenpaiMap = new HashMap<Player, Boolean>(4);

		this.ippatuMap = new HashMap<Kaze, Boolean>(4);

		this.playerMap = new HashMap<Kaze, Player>(player);

		this.yamahai = new ArrayList<Hai>(136);
		this.wanpai = new ArrayList<Hai>(14);
		this.rand = new Random();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see system.KyokuIF#init()
	 */

	public void init() {
		this.result = null;
		this.tsumoSize = 0;
		this.kanSize = 0;
		this.newDoraSize = 0;
		this.currentTurn = TON;
		this.firstTurn = true;

		this.krbuilder = null;

		this.kyokuPlayerMap.clear();
		for (Kaze kaze : Kaze.values()) {
			kyokuPlayerMap.put(kaze, new KyokuPlayer());
		}

		ippatuMap.clear();
		for (Kaze kaze : Kaze.values()) {
			ippatuMap.put(kaze, false);
		}

		// 終局情報初期化
		this.tenpaiMap.clear();
		this.agariMap.clear();

		this.atomekuriKanFlag = false;
		this.tyankanFlag = false;
		this.rinsyanFlag = false;

		this.yamahai.clear();

		// [分岐]赤ありか？
		// 赤ありルール
		if (field.getRule().isAkaAri()) {
			for (HaiType type : HaiType.values()) {
				// 数牌5は赤がある
				if (type.group3() == HaiGroup3.SU && type.number() == 5) {
					// 赤は1枚ずつ
					this.yamahai.add(MajanHai.valueOf(type, true));

					// 赤じゃない数牌5は3枚ずつ
					this.yamahai.add(MajanHai.valueOf(type, false));
					this.yamahai.add(MajanHai.valueOf(type, false));
					this.yamahai.add(MajanHai.valueOf(type, false));
				}
				// それ以外は4枚
				else {
					this.yamahai.add(MajanHai.valueOf(type, false));
					this.yamahai.add(MajanHai.valueOf(type, false));
					this.yamahai.add(MajanHai.valueOf(type, false));
					this.yamahai.add(MajanHai.valueOf(type, false));
				}
			}
		}
		// 赤なしルール
		else {
			for (HaiType type : HaiType.values()) {
				// すべて4枚ずつ
				this.yamahai.add(MajanHai.valueOf(type, false));
				this.yamahai.add(MajanHai.valueOf(type, false));
				this.yamahai.add(MajanHai.valueOf(type, false));
				this.yamahai.add(MajanHai.valueOf(type, false));
			}
		}

		this.wanpai.clear();

		// 王牌を山牌からランダムに14枚選ぶ
		for (int i = 0; i < 14; i++) {
			this.wanpai.add(fetchRandomHai(this.yamahai));
		}

		// 各プレイヤーに13枚ずつ配る
		for (Kaze kaze : Kaze.values()) {
			KyokuPlayer kp = kyokuPlayerMap.get(kaze);
			for (int i = 0; i < 13; i++) {
				kp.distribute(fetchRandomHai(yamahai));
			}
		}
	}

	/**
	 * 山牌からランダムに1牌をツモる.
	 * 
	 * @throws IllegalArgumentException 総ツモサイズが70に達しているか,ツモメソッドを連続して2回呼び出した場合.
	 */
	public void doTsumo() {
		if (isSyukyoku())
			throw new IllegalStateException("終局条件を満たしているのにdoTsumoメソッドが呼び出されました.");
		if (this.currentTumohai != null)
			throw new IllegalStateException("不正なメソッド呼び出し");
		this.currentTumohai = fetchRandomHai(this.yamahai);
		this.tsumoSize++;
	}

	/**
	 * 九種九牌の場合はtrueを返す.
	 * 
	 * @return 九種九牌の場合はtrue.
	 */
	public boolean isKyusyukyuhai() {
		if (!firstTurn) {
			return false;
		}
		return kyokuPlayerMap.get(currentTurn).isKyusyukyuhai(currentTumohai);
	}

	public void doKyusyukyuhai() {
		if (this.result != null)
			throw new IllegalStateException("既にKyokuResultは生成されている");

		Map<Player, KyokuPlayer> map = new HashMap<Player, KyokuPlayer>();
		for (Player p : playerMap.values()) {
			map.put(p, kyokuPlayerMap.get(getKazeOf(p)));
		}
		this.result = new KyokuTotyuRyukyokuResult(TotyuRyukyokuType.KYUSYUKYUHAI, playerMap.get(TON), map);
	}

	/**
	 * ツモ上がり出来る場合trueを返す.
	 * 
	 * @return ツモ上がり出来る場合true.
	 */

	public boolean isTsumoAgari() {
		// TODO check
		Set<Yaku> yaku = this.newYakuFlagSet(currentTurn, true);
		return isAgari(true, this.currentTumohai, currentTurn, yaku);
	}

	/**
	 * ツモ上がりする.
	 * @throws 既に終局している場合.
	 */
	public void doTsumoAgari() {
		if (this.result != null)
			throw new IllegalStateException("既にKyokuResultは生成されている");

		Kaze kaze = currentTurn;
		KyokuPlayer kp = kyokuPlayerMap.get(kaze);
		
		AgariResult.Builder agb = new AgariResult.Builder();
		agb.setAgariParam(new AgariParam(true, kp.isNaki(), currentTumohai, kaze, newYakuFlagSet(kaze, true)));
		agb.setField(field);
		agb.setHurohaiList(kp.getHurohaiList());
		agb.setOpenDoraList(getRealOpenDoraList());
		agb.setTehaiList(kp.getTehaiList());
		agb.setUraDoraList(getRealUraDoraList());
		agb.setYakuFlag(newYakuFlagSet(kaze, true));
		AgariResult ar = agb.build();
		
		Player agarip = playerMap.get(currentTurn);
		Player oya = playerMap.get(TON);

		Map<Player, KyokuPlayer> map = new HashMap<Player, KyokuPlayer>();
		for (Player p : playerMap.values()) {
			map.put(p, kyokuPlayerMap.get(getKazeOf(p)));
		}

		this.result = new KyokuTsumoAgariResult(currentTumohai, agarip, ar, oya, map);
	}

	/**
	 * 現在ターンの人が加槓可能の場合trueを返す.
	 */
	public boolean isKakanable() {
		if (this.kanSize == 4) {
			return false;
		}
		if (this.tsumoSize == 70) {
			return false;
		}
		KyokuPlayer kp = kyokuPlayerMap.get(currentTurn);
		return kp.isKakanable(this.currentTumohai);
	}

	/**
	 * 加槓可能な手牌のインデックスリストを返す.
	 * 
	 * @return 加槓可能な手牌のインデックスリスト.
	 */
	public List<Integer> getKakanableHaiList() {
		return kyokuPlayerMap.get(currentTurn).getKakanableHaiList(currentTumohai);
	}

	/**
	 * 指定された牌のインデックスで加槓する.ツモ牌を指定する場合13を入れる.
	 * 
	 * @param index 暗槓する牌のインデックス.13の場合,ツモ牌を表す.
	 * @return 加槓して出来た面子.
	 */
	public Mentu doKakan(int index) {
		this.openAtomekuriDora();

		KyokuPlayer kp = kyokuPlayerMap.get(currentTurn);
		Mentu mentu = null;
		// ツモ牌を加槓する場合
		if (index == 13) {
			this.currentSutehai = this.currentTumohai;
			mentu = kp.doKakan(currentTumohai);
		}
		// 手牌の中の牌で加槓する場合
		else {
			this.currentSutehai = kp.getTehai(index);
			mentu = kp.doKakan(index, currentTumohai);
		}

		this.currentTumohai = null;
		this.tyankanFlag = true;
		this.ippatuMap.put(currentTurn, false);
		this.atomekuriKanFlag = true;
		return mentu;
	}

	/**
	 * リンシャンツモする.
	 * 
	 * @throws IllegalArgumentException 総ツモサイズが70に達している場合.
	 */
	public void doRinsyanTsumo() {
		if (isSyukyoku())
			throw new IllegalStateException("終局条件を満たしているのにdoTsumoメソッドが呼び出されました.");
		this.currentTumohai = this.wanpai.get(this.kanSize++);
		this.currentSutehai = null;

		this.tsumoSize++;
		this.tyankanFlag = false;
		this.rinsyanFlag = true;
	}

	/**
	 * 暗槓可能の場合trueを返す.
	 * 
	 * @return 暗槓可能の場合true.
	 */

	public boolean isAnkanable() {
		if (isSyukyoku())
			return false;
		if (this.kanSize == 4)
			return false;
		if (this.tsumoSize == 70)
			return false;
		if (currentTumohai == null)
			return false;
		return kyokuPlayerMap.get(currentTurn).isAnkanable(currentTumohai);
	}

	/**
	 * 暗槓可能な手牌のインデックス(13はツモ牌を表す)のリストのリストを返す.
	 * 
	 * @return 暗槓可能な手牌のインデックス(13はツモ牌を表す)のリストのリスト.暗槓可能な牌が ない場合は空のリストを返す.
	 */
	public List<List<Integer>> getAnkanableHaiList() {
		if (currentTumohai == null) {
			System.err.println("Kyoku : ツモ牌がnullのときにisAnkanable()メソッドを不正に呼び出し");
			return new ArrayList<List<Integer>>(0);
		}
		return kyokuPlayerMap.get(currentTurn).getAnkanableHaiList(currentTumohai);
	}

	/**
	 * 指定されたインデックス(13はツモ牌を表す)の牌で暗槓する.
	 * 
	 * @param 暗槓する牌のインデックス(13はツモ牌を表す).
	 */
	public Mentu doAnkan(List<Integer> list) {
		this.openAtomekuriDora();
		
		KyokuPlayer kp = kyokuPlayerMap.get(currentTurn);
		Mentu m = kp.doAnkan(currentTumohai, list);

		this.newDoraSize++;
		this.firstTurn = false;
		this.currentTumohai = null;

		return m;
	}

	/**
	 * 指定された風の人が現在の捨牌に対してロンできる場合trueを返す.
	 * 
	 * @param kaze ロンできるか確かめる人の風.
	 * @return 指定された風の人が現在の捨牌に対してロンできる場合true.
	 */
	public boolean isRonable(Kaze kaze) {
		//TODO check
		if (kaze == currentTurn)
			return false;
		if (currentSutehai == null) {
			return false;
		}

		Set<Yaku> yaku = this.newYakuFlagSet(kaze, false);
		return isAgari(false, this.currentSutehai, kaze, yaku);
	}

	/**
	 * 指定された風のプレイヤーがロンあがりする.
	 * @throws IllegalStateException 既に終局している場合.
	 */
	public void doRon(Kaze kaze) {
		if (this.result != null)
			throw new IllegalStateException("既にKyokuResultは生成されている");

		KyokuPlayer kp  = kyokuPlayerMap.get(kaze);

		AgariResult.Builder agb = new AgariResult.Builder();
		agb.setAgariParam(new AgariParam(false, kp.isNaki(), currentSutehai, kaze, newYakuFlagSet(kaze, false)));
		agb.setField(field);
		agb.setHojuKaze(currentTurn);
		agb.setHurohaiList(kp.getHurohaiList());
		agb.setOpenDoraList(getRealOpenDoraList());
		agb.setTehaiList(kp.getTehaiList());
		agb.setUraDoraList(getRealUraDoraList());
		agb.setYakuFlag(newYakuFlagSet(kaze, false));
		AgariResult ar = agb.build();

		
		if (this.krbuilder == null) {
			this.krbuilder = new KyokuRonAgariResult.Builder();
		}

		this.krbuilder.put(playerMap.get(kaze), ar);
	}

	public void onRonRejected(Kaze kaze){
		kyokuPlayerMap.get(kaze).setTatyaFuritenFlag(true);
	}
	/**
	 * 指定された風の人がリーチしている場合はtrueを返す.
	 * 
	 * @param kaze 風.
	 * @return リーチしている場合true.
	 */
	public boolean isReach(Kaze kaze) {
		KyokuPlayer kp = kyokuPlayerMap.get(kaze);
		return kp.isReach() || kp.isDoubleReach();
	}

	/**
	 * 現在ターンの人がリーチ可能な場合trueを返す.
	 * 
	 * @return リーチ可能な場合true.
	 */
	public boolean isReachable() {
		if (isReach(currentTurn)) {
			return false;
		}
		if (isLastTurn())
			return false;
		KyokuPlayer kp = kyokuPlayerMap.get(currentTurn);
		if (kp.isNaki())
			return false;
		//TODO isTenpai
		return isTenpai();
	}

	/**
	 * 残りツモ枚数が4以下の場合trueを返す．
	 * @return 最後の順のである場合はtrue．
	 */
	public boolean isLastTurn() {
		return tsumoSize - 66 >= 0;
	}

	/**
	 * リーチできる場合,その牌を切ってリーチ出来るもののインデックスリストを返す.
	 * 
	 * @return 不要牌インデックスリスト.
	 */
	public List<Integer> getReachableHaiList() {
		//TODO check
		Kaze kaze = getCurrentTurn();
		KyokuPlayer kp = kyokuPlayerMap.get(kaze);

		TehaiList tehaiList = kp.getTehaiList();
		if(kp.isNaki())
			return new ArrayList<Integer>(0);
		
		return AgariMethods.getReachableIndexList(tehaiList, currentTumohai, field);
	}

	/**
	 * リーチする.初巡の場合はダブルリーチする.
	 */
	public void doReach() {
		ippatuMap.put(currentTurn, true);
		KyokuPlayer kp = kyokuPlayerMap.get(currentTurn);
		kp.doReach(firstTurn);
	}

	/**
	 * 指定されたインデックス(13はツモ牌)の牌を切る.ツモ切りの場合はdiscardTsumoHaiメソッドを用いる.
	 * 
	 * @param index 手牌のインデックス(13はツモ牌).
	 */
	public void discard(int index) {
		KyokuPlayer kp = kyokuPlayerMap.get(currentTurn);
		this.currentSutehai = kp.discard(index, currentTumohai);
		this.kiru();
	}

	/**
	 * ツモ切りする.
	 */
	public void discardTsumoHai() {
		KyokuPlayer kp = kyokuPlayerMap.get(currentTurn);
		this.currentSutehai = kp.discard(13, currentTumohai);
		this.ippatuMap.put(currentTurn, false);
		this.kiru();
	}

	/**
	 * 牌を切る際に起こる副作用。
	 */
	private void kiru() {
		this.currentTumohai = null;
		this.rinsyanFlag = false;
	}

	/**
	 * 指定された風のプレイヤーが明槓できる場合trueを返す.
	 * 
	 * @param kaze プレイヤーの風.
	 * @return 明槓できる場合true.
	 */
	public boolean isMinkanable(Kaze kaze) {
		if (currentSutehai == null) {
			return false;
		}
		if (kaze == currentTurn || this.kanSize == 4)
			return false;
		return kyokuPlayerMap.get(kaze).isMinkanable(currentSutehai.type());
	}

	/**
	 * 指定された風のプレイヤーの明槓できる手牌のインデックスのリストを返す.
	 * 
	 * @return 明槓できる手牌のインデックスのリスト.
	 */
	public List<Integer> getMinkanableList(Kaze kaze) {
		if (currentSutehai == null) {
			System.err.println("Kyoku : currentSutehaiがnullのときにこのメソッドを呼び出してはならない.");
			return new ArrayList<Integer>(0);
		}
		return kyokuPlayerMap.get(kaze).getMinkanableList(currentSutehai.type());
	}

	/**
	 * 指定された風のプレイヤーが明槓をする.
	 * 
	 * @param kaze 風.
	 * @return 明槓して出来た面子.
	 */
	public Mentu doMinkan(Kaze kaze) {
		assert isMinkanable(kaze);

		KyokuPlayer ckp = kyokuPlayerMap.get(currentTurn);
		KyokuPlayer nkp = kyokuPlayerMap.get(kaze);

		Mentu m = nkp.doMinkan(currentSutehai, currentTurn);
		ckp.addSutehai(new Sutehai(currentSutehai, currentTurn));

		this.naku();
		this.currentTurn = kaze;
		kyokuPlayerMap.get(currentTurn).setTatyaFuritenFlag(false);
		this.atomekuriKanFlag = true;
		return m;
	}

	/**
	 * 指定された風のプレイヤーがポンできる場合trueを返す.
	 * 
	 * @param kaze 風.
	 * @return ポンできる場合true.
	 */
	public boolean isPonable(Kaze kaze) {
		if (currentSutehai == null) {
			return false;
		}
		if (kaze == currentTurn)
			return false;
		return kyokuPlayerMap.get(kaze).isPonable(currentSutehai.type());
	}

	/**
	 * 指定された風のプレイヤーのポン出来る牌インデックスのリストのリストを返す.
	 * 
	 * @param kaze 風.
	 * @return 牌インデックスのリストのリスト.
	 */
	public List<List<Integer>> getPonableHaiList(Kaze kaze) {
		return kyokuPlayerMap.get(kaze).getPonableHaiList(currentSutehai.type());
	}

	/**
	 * ポンする.
	 * 
	 * @param kaze 風.
	 * @param ponList ポンするインデックスリスト.
	 * @return ポンして出来た面子.
	 */
	public Mentu doPon(Kaze kaze, List<Integer> ponList) {
		assert isPonable(kaze);
		assert ponList.size() == 2;

		KyokuPlayer nkp = kyokuPlayerMap.get(kaze);
		KyokuPlayer ckp = kyokuPlayerMap.get(currentTurn);
		Mentu m = nkp.doPon(ponList, currentSutehai, currentTurn);

		ckp.addSutehai(new Sutehai(currentSutehai, kaze));

		this.naku();
		this.currentTurn = kaze;
		kyokuPlayerMap.get(currentTurn).setTatyaFuritenFlag(false);
		return m;
	}

	/**
	 * チーできる場合trueを返す.
	 * 
	 * @return チーできる場合true.
	 */
	public boolean isChiable() {
		if (currentSutehai == null) {
			return false;
		}
		if (currentSutehai == null)
			return false;
		return kyokuPlayerMap.get(currentTurn.simo()).isChiable(currentSutehai.type());
	}

	/**
	 * チー出来る牌インデックスのリストのリストを返す.
	 * 
	 * @return チー出来る牌インデックスのリストのリスト.
	 */
	public List<List<Integer>> getChiableHaiList() {
		return kyokuPlayerMap.get(currentTurn.simo()).getChiableHaiList(currentSutehai.type());
	}

	/**
	 * チーする.
	 * 
	 * @param tiList チーする牌インデックスのリスト.
	 * @return チーして出来た面子.
	 */
	public Mentu doChi(List<Integer> tiList) {
		assert isChiable();
		assert tiList.size() == 2;

		Kaze next = currentTurn.simo();
		KyokuPlayer nkp = kyokuPlayerMap.get(next);
		KyokuPlayer ckp = kyokuPlayerMap.get(currentTurn);

		Mentu m = nkp.doChi(tiList, currentSutehai, currentTurn);
		ckp.addSutehai(new Sutehai(currentSutehai, next));

		this.naku();
		this.currentTurn = next;
		kyokuPlayerMap.get(currentTurn).setTatyaFuritenFlag(false);
		return m;
	}

	/**
	 * 鳴いたときの副作用。
	 */
	private void naku() {
		this.openAtomekuriDora();
		this.currentSutehai = null;
		this.firstTurn = false;
		for (Kaze kaze : Kaze.values()) {
			this.ippatuMap.put(kaze, false);
		}
	}

	/**
	 * 現在のターンを次のプレイヤーに移す.
	 */
	public void nextTurn() {
		this.openAtomekuriDora();
		if (this.firstTurn) {
			if (this.tsumoSize >= 4)
				this.firstTurn = false;
		}
		kyokuPlayerMap.get(currentTurn).addSutehai(new Sutehai(currentSutehai));
		this.currentSutehai = null;
		this.currentTurn = this.currentTurn.simo();
		KyokuPlayer kp = kyokuPlayerMap.get(currentTurn);
		if(!kp.isReach()){
			kp.setTatyaFuritenFlag(false);
		}
	}

	/**
	 * 指定された風の人がフリテンになっている場合trueを返す.
	 * このメソッドは同順フリテンも判定する.
	 * 
	 * @param kaze 風.
	 * @return フリテンの場合true.
	 */
	public boolean isFuriten(Kaze kaze) {
		return kyokuPlayerMap.get(kaze).isFuriten();
	}
	
	// TODO
	public boolean isFuriten(Kaze kaze, HaiType hai) {
		return false;
	}

	/**
	 * 現在ターンのプレイヤーがツモ牌をいれてテンパイしている場合trueを返す.
	 * 
	 * @return テンパイしている場合true.
	 */
	public boolean isTenpai() {
		KyokuPlayer kp = kyokuPlayerMap.get(currentTurn);
		return AgariMethods.isTenpaiWithExtra(kp.getTehaiList(), this.currentTumohai, field);
	}

	/**
	 * 指定された風のプレイヤーがテンパイしている場合trueを返す.
	 * 
	 * @param kaze 風.
	 * @return テンパイしている場合true.
	 */
	public boolean isTenpai(Kaze kaze) {
		KyokuPlayer kp = kyokuPlayerMap.get(kaze);
		return AgariMethods.isTenpai(kp.getTehaiList(), kp.isNaki());
	}

	public void openAtomekuriDora() {
		if (atomekuriKanFlag) {
			this.newDoraSize++;
			this.atomekuriKanFlag = false;
		}
	}

	/**
	 * 三家和の場合trueを返す.
	 * @return 三家和の場合はtrue.
	 */
	public boolean isSanchaho() {
		if (krbuilder == null)
			return false;

		return krbuilder.size() == 3;
	}

	/**
	 * 四風連打の場合trueを返す.
	 * 
	 * @return 四風連打の場合true.
	 */
	public boolean isSufontsuRenta() {
		if (this.firstTurn == false)
			return false;
		if (this.tsumoSize != 4)
			return false;

		HaiType haiType = null;
		for (Kaze kaze : Kaze.values()) {
			if (kaze == PE)
				continue;
			if (haiType == null) {
				HaiType type = kyokuPlayerMap.get(kaze).getSutehai(0).type();
				if (type.group3() != HaiGroup3.KAZE)
					return false;
				haiType = type;
			} else {
				if (haiType != kyokuPlayerMap.get(kaze).getSutehai(0).type()) {
					return false;
				}
			}
		}
		if (haiType != currentSutehai.type()) {
			return false;
		}
		return true;
	}

	/**
	 * 四開槓の場合trueを返す.ただし一人のプレイヤーが4回槓している場合はfalseとなる.
	 * 
	 * @return 四開槓の場合true.
	 */
	public boolean isSukaikan() {
		if (this.kanSize != 4) {
			for (Kaze kaze : kyokuPlayerMap.keySet()) {
				if (kyokuPlayerMap.get(kaze).getKanSize() == 4)
					return true;
			}
			return false;
		}
		return true;
	}

	/**
	 * 四家リーチの場合trueを返す.
	 * 
	 * @return 四家リーチの場合true.
	 */
	public boolean isSuchaReach() {
		for (Kaze kaze : kyokuPlayerMap.keySet()) {
			KyokuPlayer kp = kyokuPlayerMap.get(kaze);
			if (!kp.isReach() && !kp.isDoubleReach())
				return false;
		}
		return true;
	}

	public void doSuchaReach() {
		if (this.result != null)
			throw new IllegalStateException("既にKyokuResultは生成されている");

		Map<Player, KyokuPlayer> map = new HashMap<Player, KyokuPlayer>();
		for (Player p : playerMap.values()) {
			map.put(p, kyokuPlayerMap.get(getKazeOf(p)));
		}
		this.result = new KyokuTotyuRyukyokuResult(TotyuRyukyokuType.SUCHAREACH, playerMap.get(TON), map);
	}

	/**
	 * リンシャン牌を合わした総ツモ枚数が70枚に達していたらtrueを返す.
	 * 
	 * @return 総ツモ枚数が70枚に達していたらtrue.
	 */
	public boolean isRyukyoku() {
		return this.tsumoSize == 70;
	}

	/**
	 * 流局する.ここでいう流局とは総ツモ枚数が70枚に達して,ツモれる牌がなくなったときの 流局を指す.
	 * このメソッドを呼び出すことでKyokuResutlを取得できるようになる.
	 */
	public void doRyukyoku() {
		if (this.result != null)
			throw new IllegalStateException("既にKyokuResultは生成されている");

		Map<Player, KyokuPlayer> map = new HashMap<Player, KyokuPlayer>();
		for (Player p : playerMap.values()) {
			map.put(p, kyokuPlayerMap.get(getKazeOf(p)));
		}
		this.result = new KyokuRyukyokuResult(playerMap.get(TON), map);
	}

	public boolean isTotyuRyukyoku() {
		if (this.result == null)
			return false;

		return result.isTotyuRyukyoku();
	}

	/**
	 * 途中流局(三家和)する.
	 * @throw IllegalStateException 既に局が終わっている場合.
	 */
	public void doTotyuRyukyokuSanchaho() {
		if (this.result != null)
			throw new IllegalStateException("既にKyokuResultは生成されている");

		Map<Player, KyokuPlayer> map = new HashMap<Player, KyokuPlayer>();
		for (Player p : playerMap.values()) {
			map.put(p, kyokuPlayerMap.get(getKazeOf(p)));
		}
		this.result = new KyokuTotyuRyukyokuResult(TotyuRyukyokuType.SANCHAHO, playerMap.get(TON), map);
	}

	/**
	 * この局が終局している場合falseを返す.
	 * 
	 * @return 終局している場合false.
	 */
	public boolean isSyukyoku() {
		return result != null || krbuilder != null;
	}

	// TODO いらない？
	public void doSyukyoku() {
		// TODO inserted
	}

	/**
	 * この局のルールを返す.
	 * 
	 * @return この局のルール.
	 */
	public Rule getRule() {
		return field.getRule();
	}

	/**
	 * 風->プレイヤーを表すマップを返す.
	 * 
	 * @return 風->プレイヤーを表すマップ.
	 */
	public Map<Kaze, Player> getPlayerMap() {
		return new HashMap<Kaze, Player>(playerMap);
	}

	/**
	 * 指定された風のプレイヤーを返す.
	 * 
	 * @param kaze 風.
	 * @return プレイヤー.
	 */
	public Player getPlayer(Kaze kaze) {
		return this.playerMap.get(kaze);
	}

	/**
	 * この局の場風を返す.
	 * 
	 * @return この局場風.
	 */
	public Kaze getBakaze() {
		return field.getBakaze();
	}

	/**
	 * この局が終局している場合,その局結果オブジェクトを返す.終局していない場合はnullを返す.</br>
	 * 局が終局するためには次の3つのうちのどれかを呼び出す必要がある.</br>
	 * 　・doRyukoyku()</br>
	 * 　・doTotyuRyukyoku()</br>
	 * 　・doRonAgari()</br>
	 * 　・doTsumoAgari()</br>
	 * 
	 * @return 局結果オブジェクト.
	 * @throws IllegalStateException まだ終局していない場合.
	 */
	public KyokuResult createKyokuResult() {
		if (result != null)
			return result;
		if (result == null && krbuilder != null) {
			Map<Player, KyokuPlayer> map = new HashMap<Player, KyokuPlayer>();
			for (Player p : playerMap.values()) {
				map.put(p, kyokuPlayerMap.get(getKazeOf(p)));
			}
			Player oya = playerMap.get(TON);
			Player hoju = playerMap.get(currentTurn);
			return result = krbuilder.build(currentSutehai, hoju, oya, map);
		}
		throw new IllegalStateException("KyokuResultはまだ生成されていない");
	}

	/**
	 * この局の開かれているドラ表示牌リストを返す.
	 * 
	 * @return ドラ表示牌リスト.
	 */
	public List<Hai> getOpenDoraList() {
		List<Hai> list = new ArrayList<Hai>();
		for (int i = 0; i < this.newDoraSize + 1; i++) {
			list.add(this.wanpai.get(4 + 2 * i));
		}
		return list;
	}

	/**
	 * この局の開かれている実際のドラの牌種リストを返す.
	 * 
	 * @return ドラの牌種リスト.
	 */
	public List<HaiType> getRealOpenDoraList() {
		List<HaiType> list = new ArrayList<HaiType>();
		for (int i = 0; i < this.newDoraSize + 1; i++) {
			list.add(this.wanpai.get(4 + 2 * i).nextOfDora());
		}
		return list;
	}

	/**
	 * この局の裏ドラ表示牌リストを返す.
	 * 
	 * @return 裏ドラ表示牌リスト.
	 */
	public List<Hai> getUraDoraList() {
		List<Hai> list = new ArrayList<Hai>();
		for (int i = 0; i < this.newDoraSize + 1; i++) {
			list.add(this.wanpai.get(4 + 2 * i + 1));
		}
		return list;
	}

	/**
	 * この局の実際の裏ドラの牌種リストを返す.
	 * 
	 * @return 裏ドラの牌種リスト.
	 */
	public List<HaiType> getRealUraDoraList() {
		List<HaiType> list = new ArrayList<HaiType>();
		for (int i = 0; i < this.newDoraSize + 1; i++) {
			list.add(this.wanpai.get(4 + 2 * i + 1).type().nextOfDora());
		}
		return list;
	}

	/**
	 * この局の実際のドラ(表と裏)リストを返す．
	 * @return この局の実際のドラ(表と裏)リスト.
	 */
	public List<HaiType> getRealAllDoraList() {
		List<HaiType> list = new ArrayList<HaiType>();
		for (int i = 0; i < this.newDoraSize + 1; i++) {
			list.add(this.wanpai.get(4 + 2 * i).nextOfDora());
			list.add(this.wanpai.get(4 + 2 * i + 1).nextOfDora());
		}
		return list;
	}

	// TODO java doc
	/**
	 * 
	 * @param kaze
	 * @return
	 */
	public List<Mentu> getFuroHaiList(Kaze kaze) {
		return kyokuPlayerMap.get(kaze).getHurohaiList();
	}

	/**
	 * 山牌からランダムに1牌とってくる.
	 * 
	 * @param list 山牌リスト.
	 * @return 山牌からランダムに選ばれた牌.
	 */
	private Hai fetchRandomHai(List<Hai> list) {
		return list.remove(rand.nextInt(list.size()));
	}

	/**
	 * 各プレイヤーの手牌をソートする.
	 */
	public void sortTehaiList() {
		HaiComparator comp = Hai.HaiComparator.ASCENDING_ORDER;
		for (Kaze kaze : Kaze.values()) {
			kyokuPlayerMap.get(kaze).sortTehai(comp);
		}
	}

	/**
	 * 指定されたプレイヤーの手牌をソートする.
	 * 
	 * @param kaze 風.
	 */
	public void sortTehaiList(Kaze kaze) {
		HaiComparator comp = Hai.HaiComparator.ASCENDING_ORDER;
		kyokuPlayerMap.get(kaze).sortTehai(comp);
	}

	/**
	 * 現在ターンの風を返す.
	 * @return 現在ターンの風.
	 */
	public Kaze getCurrentTurn() {
		return this.currentTurn;
	}

	/**
	 * 現在ターンのプレイヤーを返す.
	 * @return 現在ターンのプレイヤー
	 */
	public Player getCurrentPlayer() {
		return playerMap.get(currentTurn);
	}

	/**
	 * 指定された風のプレイヤーの手牌リストを返す. 手牌リストは防御的コピーする.
	 * 
	 * @param kaze 風.
	 * @return 手牌リスト.
	 */
	public TehaiList getTehaiList(Kaze kaze) {
		return kyokuPlayerMap.get(kaze).getTehaiList();
	}

	/**
	 * 指定された風のプレイヤーの捨て牌リストを返す. 手牌リストは防御的コピーする.
	 * 
	 * @param kaze プレイヤーの風.
	 * @return 捨て牌リスト.
	 */
	public SutehaiList getSutehaiList(Kaze kaze) {
		return kyokuPlayerMap.get(kaze).getSutehaiList();
	}

	/**
	 * 指定された風のプレイヤーの副露牌リストを返す. 手牌リストは防御的コピーする.
	 * 
	 * @param kaze プレイヤーの風.
	 * @return 副露牌リスト.
	 */
	public HurohaiList getHurohaiList(Kaze kaze) {
		return kyokuPlayerMap.get(kaze).getHurohaiList();
	}

	public Map<Kaze, SutehaiList> getSutehaiMap() {
		Map<Kaze, SutehaiList> ret = new HashMap<Kaze, SutehaiList>(4);
		for (Kaze kaze : Kaze.values()) {
			ret.put(kaze, kyokuPlayerMap.get(kaze).getSutehaiList());
		}
		return ret;
	}

	public Map<Kaze, HurohaiList> getHurohaiMap() {
		Map<Kaze, HurohaiList> ret = new HashMap<Kaze, HurohaiList>(4);
		for (Kaze kaze : Kaze.values()) {
			ret.put(kaze, kyokuPlayerMap.get(kaze).getHurohaiList());
		}
		return ret;
	}

	/**
	 * 指定されたプレイヤーの風を返す.
	 * 
	 * @param p プレイヤー.
	 * @return 指定されたプレイヤーの風.
	 */
	public Kaze getKazeOf(Player p) {
		for (Kaze kaze : playerMap.keySet()) {
			if (playerMap.get(kaze).equals(p))
				return kaze;
		}
		return null;
	}

	/**
	 * 現在のツモ牌を返す.
	 * 
	 * @return 現在のツモ牌.
	 */
	public Hai getCurrentTsumoHai() {
		return this.currentTumohai;
	}

	/**
	 * 現在の捨牌を返す.
	 * 
	 * @return 現在の捨牌.
	 */
	public Hai getCurrentSutehai() {
		return this.currentSutehai;
	}

	/**
	 * この局全員の槓の回数を返す.
	 * 
	 * @return 槓の回数.
	 */
	public int getKanSize() {
		return this.kanSize;
	}

	/**
	 * あがれるかどうかを判定
	 * 
	 * @param tumo ツモかどうか
	 * @param agariHai あがり牌
	 * @param kaze 判定するプレイヤの風
	 * @param flagCheckYakuSet フラグによる役判定
	 * @return あがれるならtrue
	 */
	private boolean isAgari(boolean tumo, Hai agariHai, Kaze kaze, Set<Yaku> flagCheckYakuSet) {
		if (isFuriten(kaze, agariHai.type())) {
//		if (isFuriten(kaze) && !tumo) {
			return false;
		}

		KyokuPlayer kp = kyokuPlayerMap.get(kaze);
		return AgariMethods.isAgari(kp.getTehaiList(), kp.getHurohaiList(), agariHai, flagCheckYakuSet, tumo, kp.isNaki(), kaze, field);
	}

	/**
	 * この局現在の役フラグセットを生成して,それを返す.
	 * 
	 * @param kaze 自風.
	 * @param tumo ツモ.
	 * @return 役フラグセット.
	 */
	public Set<Yaku> newYakuFlagSet(Kaze kaze, boolean tumo) {
		Set<Yaku> set = new HashSet<Yaku>();

		KyokuPlayer kp = kyokuPlayerMap.get(kaze);
		if (kp.isReach()) {
			set.add(NormalYaku.RICHI);
		}
		if (tumo && !kp.isNaki()) {
			set.add(NormalYaku.TSUMO);
		}
		if (this.ippatuMap.get(kaze)) {
			set.add(NormalYaku.IPPATSU);
		}
		if (!tumo && this.tsumoSize == 70) {
			set.add(NormalYaku.HOTEI);
		}
		if (tumo && tsumoSize == 70) {
			set.add(NormalYaku.HAITEI);
		}
		if (tumo && rinsyanFlag) {
			set.add(NormalYaku.RINSYANKAIHO);
		}
		if (!tumo && tyankanFlag) {
			set.add(NormalYaku.TYANKAN);
		}
		if (kp.isDoubleReach()) {
			set.add(NormalYaku.DABURURICHI);
		}
		if (tumo && firstTurn && kaze == TON) {
			set.add(Yakuman.TENHO);
		}
		if (tumo && firstTurn && kaze != TON) {
			set.add(Yakuman.CHIHO);
		}
		return set;
	}

	public int sizeOfYamahai() {
		return yamahai.size();
	}

	/**
	 * 指定された風のプレイヤーの手牌リストのサイズを返す.
	 * 
	 * @param kaze プレイヤーの風.
	 * @return 手牌リストのサイズ.
	 */
	public int sizeOfTehai(Kaze kaze) {
		return kyokuPlayerMap.get(kaze).sizeOfTehai();
	}

	// DEBUG
	public void disp() {
		System.out.println("場風：" + field.getBakaze());
		System.out.println(this.tsumoSize + "順目");
		System.out.println("");
		System.out.println("山牌(" + this.yamahai.size() + ")：" + this.yamahai);
		System.out.println("王牌(" + this.wanpai.size() + ")：" + this.wanpai);
		System.out.println("ドラ：" + getRealAllDoraList());
		System.out.println("");
		for (Kaze kaze : Kaze.values()) {
			if (currentTurn == kaze) {
				System.out.println("→風：" + kaze);
				System.out.println("\tツモ牌：" + currentTumohai);
			} else {
				System.out.println("　風：" + kaze);
			}
			kyokuPlayerMap.get(kaze).disp();
		}
		System.out.println(String.format("現在ターン：%s, ツモ牌：%s", currentTurn, currentTumohai));
	}

	// DEBUG
	public void removeYamahai(Collection<Hai> c) {
		for (Hai hai : c) {
			this.yamahai.remove(hai);
		}
	}

	//DEBUG
	public void removeWanpai(Collection<Hai> c) {
		for (Hai hai : c) {
			this.wanpai.remove(hai);
		}
	}

	// DEBUG
	public void setKyokuPlayer(Kaze kaze, KyokuPlayer kp) {
		this.kyokuPlayerMap.put(kaze, kp);
	}

	// DEBUG
	public void doTsumo(Hai tsumohai) {
		if (isSyukyoku())
			throw new IllegalStateException("終局条件を満たしているのにdoTsumoメソッドが呼び出されました.");
		if (this.currentTumohai != null)
			throw new IllegalStateException("ツモ牌がnullでない場合にdoTsumoメソッドを呼び出せない");
		if (!this.yamahai.remove(tsumohai)) {
			//			throw new IllegalArgumentException("この牌は山に存在しない : " + tsumohai);
		}
		this.currentTumohai = tsumohai;
		this.tsumoSize++;
	}
	
	public List<Hai> getWanpaiList(){
		return wanpai;
	}
	
	public List<Hai> getYamahaiList(){
		return yamahai;
	}
}
