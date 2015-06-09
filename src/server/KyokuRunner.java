package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import system.Hai;
import system.HurohaiList;
import system.Kaze;
import system.Kyoku;
import system.Mentu;
import system.Player;
import system.Sutehai;
import system.SutehaiList;
import system.TehaiList;
import test.Console;
import ai.AI;
import ai.AIType01;
import ai.AIType_Debug;

/**
 * 1蝗槭�螻�ｒ襍ｰ繧峨○繧九け繝ｩ繧ｹ.
 */
public class KyokuRunner {
	private final Kyoku kyoku;

	private final Map<Kaze, Player> playerMap;
	private final Map<Kaze, AI> aiMap;
	private final Map<Kaze, Transporter> transporterMap;

	private int stateCode;
	private boolean discardFlag;

	/**
	 * 謖�ｮ壹＆繧後◆螻�ｒ蜍輔°縺吝ｱ�Λ繝ｳ繝翫�縺ｮ繧ｳ繝ｳ繧ｹ繝医Λ繧ｯ繧ｿ.
	 * 
	 * @param kyoku
	 *            螻�が繝悶ず繧ｧ繧ｯ繝�
	 */
	public KyokuRunner(Kyoku kyoku, Map<Player, Transporter> trMap) {
		this.kyoku = kyoku;
		this.playerMap = kyoku.getPlayerMap();
		this.aiMap = new HashMap<Kaze, AI>(4);
		for (Player p : playerMap.values()) {
			if (p.isMan())
				continue;
			AI ai = new AIType_Debug(p, false, true);
			ai.update(kyoku);
			this.aiMap.put(kyoku.getKazeOf(p), ai);
		}

		this.transporterMap = new HashMap<Kaze, Transporter>(4);
		for (Player p : trMap.keySet()) {
			this.transporterMap.put(kyoku.getKazeOf(p), trMap.get(p));
		}
	}

	/**
	 * 螻�ｒ髢句ｧ九☆繧�
	 */
	public void run() {
		kyoku.init();

		stateCode = STATE_CODE_TSUMO;

		kyokuLoop: while (true) {
			System.out.println("繧ｿ繝ｼ繝ｳ繝√ぉ繝ｳ繧ｸ");
			playerLoop: while (true) {
				switch (stateCode) {
				case STATE_CODE_TSUMO:
					initTransporterFlag();
					initDiscardFlag();
					doTsumo();
					kyoku.sortTehaiList();
					kyoku.disp();
					sendNeededInformation();
					stateCode = STATE_CODE_SEND;
					break;

				case STATE_CODE_SEND:
					sendBeforeDiscard();
					stateCode = STATE_CODE_KYUSYUKYUHAI;
					break;

				case STATE_CODE_KYUSYUKYUHAI:
					doKyusyuKyuhai();
					break;

				case STATE_CODE_TSUMOAGARI:
					doTsumoAgari();
					break;

				case STATE_CODE_KAKAN:
					doKakan();
					sendNeededInformation();
					break;

				case STATE_CODE_CHANKANRON:
					doChankanRonAgari();
					break;

				case STATE_CODE_RINSYANTSUMO:
					initTransporterFlag();
					onDiscardFlag();
					doRinsyanTsumo();
					sendNeededInformation();
					stateCode = STATE_CODE_SEND;
					break;

				case STATE_CODE_ANKAN:
					doAnkan();
					sendNeededInformation();
					break;

				case STATE_CODE_ISREACH:
					reach();
					sendNeededInformation();
					break;

				case STATE_CODE_DISCARD:
					initTransporterFlag();
					kyoku.sortTehaiList();
					kyoku.disp();
					discard();
					sendNeededInformation();
					stateCode = STATE_CODE_RON;
					break;
				case STATE_CODE_RON:
					sendAfterDiscard();
					doRonAgari();
					sendNeededInformation();
					break;

				case STATE_CODE_SUCHA:
					isSucha(kyoku);
					break;

				case STATE_CODE_NAKI:
					doMinkan();
					sendNeededInformation();
					doPon();
					sendNeededInformation();
					doChi();
					sendNeededInformation();
					onDiscardFlag();
					if (stateCode == STATE_CODE_NAKI) {
						stateCode = STATE_CODE_NEXTTURN;
					}
					break;

				case STATE_CODE_NEXTTURN:
					if (kyoku.isRyukyoku()) {
						doTempai();//(莉ｮ縺ｫ縺薙％縺ｫ縺翫＞縺ｦ縺�ｋ縺�¢)
						kyoku.doRyukyoku();
						stateCode = STATE_CODE_ENDOFKYOKU;
					} else {
						kyoku.nextTurn();
						stateCode = STATE_CODE_TSUMO;
					}
					break playerLoop;
				case STATE_CODE_ENDOFKYOKU:
					break kyokuLoop;

				default:
					break;
				}
			}
		}
	}

	/**
	 * 蜷�け繝ｩ繧､繧｢繝ｳ繝医↓蝣ｴ(謐ｨ縺ｦ迚�蜑ｯ髴ｲ迚�謇狗煙縺ｪ縺ｩ)縺ｮ諠��繧帝�繧�
	 */
	private void sendNeededInformation() {
		Map<Kaze, HurohaiList> nakihai = kyoku.getHurohaiMap();

		Map<Kaze, List<Hai>> sutehai = new HashMap<Kaze, List<Hai>>();
		for (Kaze kaze : Kaze.values()) {
			SutehaiList sutehailist = kyoku.getSutehaiList(kaze);
			sutehai.put(kaze, sutehailist.toNakiExcludedHaiList());
		}

		List<Integer> tehaiSize = new ArrayList<Integer>();
		for (Kaze kaze : Kaze.values()) {
			tehaiSize.add(kyoku.getTehaiList(kaze).size());
		}

		for (Kaze kaze : transporterMap.keySet()) {
			TehaiList tehai = kyoku.getTehaiList(kaze);
			Server tr = transporterMap.get(kaze);

			tr.sendField(tehai, nakihai, sutehai, kyoku.getCurrentTurn(), kyoku
					.getCurrentSutehai(), tehaiSize, kyoku.getYamahaiList()
					.size(), kyoku.getWanpaiList().size(), kyoku
					.getOpenDoraList());
		}
	}

	/**
	 * 繝�Δ繧定｡後＞,繝�Δ繧定｡後▲縺溘％縺ｨ繧偵け繝ｩ繧､繧｢繝ｳ繝医↓遏･繧峨○繧�
	 */
	private void doTsumo() {
		kyoku.doTsumo();
		Server tr = transporterMap.get(kyoku.getCurrentTurn());
		if (tr != null) {
			tr.sendTsumoHai(kyoku.getCurrentTsumoHai());
		}
	}

	// 蠍ｺ荳翫ヤ繝｢縺吶ｋ繝｡繧ｽ繝�ラ
	private void doRinsyanTsumo() {
		kyoku.doRinsyanTsumo();
		kyoku.sortTehaiList();
		kyoku.disp();

		Server tr = transporterMap.get(kyoku.getCurrentTurn());
		if (tr != null) {
			tr.sendTsumoHai(kyoku.getCurrentTsumoHai());
		}
	}

	/**
	 * 荵晉ｨｮ荵晉煙,繝�Δ荳翫′繧�證玲ｧ�蜉�ｧ�繝ｪ繝ｼ繝√′縺ｧ縺阪ｋ縺ｨ縺阪↓,縺昴ｌ繧帝�菫｡縺吶ｋ.
	 */
	private void sendBeforeDiscard() {
		Player p = kyoku.getCurrentPlayer();
		if (p.isMan()) {
			Server trserver = transporterMap.get(kyoku.getCurrentTurn());
			if (kyoku.isKyusyukyuhai())
				trserver.requestKyusyukyuhai();
			if (kyoku.isTsumoAgari())
				trserver.requestTsumoAgari();
			if (kyoku.isKakanable())
				trserver.sendKakanableIndexList(kyoku.getKakanableHaiList());
			if (kyoku.isAnkanable())
				trserver.sendAnkanableIndexLists(kyoku.getAnkanableHaiList());
			if (kyoku.isReachable())
				trserver.sendReachableIndexList(kyoku.getReachableHaiList());
			Transporter tr = transporterMap.get(kyoku.getCurrentTurn());
			trserver.sendDiscard(isThereTsumohai(tr));
		} else {
			// TODO AI縺ｮ蝣ｴ蜷医�菴輔ｂ縺励↑縺�ｼ�
		}
	}

	// 繝ｭ繝ｳ,繝√�,繝昴Φ,譏取ｧ薙ｒ騾√ｋ
	private void sendAfterDiscard() {
		sendRonAgari();
		sendMinkan();
		sendPon();
		sendChi();
	}

	/**
	 * 荵晉ｨｮ荵晉煙縺後〒縺阪ｋ縺ｨ縺�wait繧貞他縺ｳ蜃ｺ縺�
	 */
	private void doKyusyuKyuhai() {
		if (kyoku.isKyusyukyuhai()) {
			Player p = kyoku.getCurrentPlayer();
			if (p.isMan()) {
				Transporter tr = transporterMap.get(kyoku.getCurrentTurn());
				if (waitKyusyukyuhai(tr)) {
					kyoku.doKyusyukyuhai();
					stateCode = STATE_CODE_ENDOFKYOKU;
					return;
				}
				// while loop 繧呈栢縺代ｋ
			} else {
				AI ai = aiMap.get(kyoku.getCurrentTurn());
				if (ai.isKyusyukyuhai()) {
					kyoku.doKyusyukyuhai();
					stateCode = STATE_CODE_ENDOFKYOKU;
					return;
				}
				// AI
			}
		}
		stateCode = STATE_CODE_TSUMOAGARI;
	}

	// 繝�Δ縺ゅ′繧翫′縺ｧ縺阪ｋ縺ｨ縺�wait繧貞他縺ｳ蜃ｺ縺�
	private void doTsumoAgari() {
		if (kyoku.isTsumoAgari()) {
			Player p = kyoku.getCurrentPlayer();
			if (p.isMan()) {
				Transporter tr = transporterMap.get(kyoku.getCurrentTurn());
				boolean tsumoagari = waitTsumoagari(tr);
				if (tsumoagari) {
					kyoku.doTsumoAgari();
					kyoku.doSyukyoku();
					System.out.println(kyoku.getCurrentPlayer() + " : つも！！");
					Console.wairEnter();
					stateCode = STATE_CODE_ENDOFKYOKU;
					return;
				} else {
					stateCode = STATE_CODE_KAKAN;
					return;
				}
			} else {
				AI ai = aiMap.get(kyoku.getCurrentTurn());
				if (ai.isTumoAgari()) {
					kyoku.doTsumoAgari();
					kyoku.doSyukyoku();
					System.out.println(kyoku.getCurrentPlayer() + " : 繝�Δ縺｡繧�▲縺滂ｼ�");
					Console.wairEnter();
					stateCode = STATE_CODE_ENDOFKYOKU;
					return;
				}
			}
		}
		stateCode = STATE_CODE_KAKAN;
	}

	// 蜉�ｧ薙〒縺阪ｋ縺ｨ縺�wait繧貞他縺ｳ蜃ｺ縺�
	private void doKakan() {
		if (kyoku.isKakanable()) {
			Player p = kyoku.getCurrentPlayer();
			if (p.isMan()) {
				Transporter tr = transporterMap.get(kyoku.getCurrentTurn());
				int kakanindex = waitKakan(tr, kyoku.getKakanableHaiList());
				if (kakanindex != -1) {
					Mentu kakanMentu = kyoku.doKakan(kakanindex);
					notifyNaki(p, kakanMentu);
					stateCode = STATE_CODE_CHANKANRON;
					return;
				}
			} else {
				AI ai = aiMap.get(kyoku.getCurrentTurn());
				int index = -1;
				if ((index = ai.kakan(kyoku.getKakanableHaiList())) != -1) {
					Mentu kakanMentu = kyoku.doKakan(kyoku
							.getKakanableHaiList().get(index));
					notifyNaki(p, kakanMentu);
					stateCode = STATE_CODE_CHANKANRON;
					return;
				}
				// AI
			}
		}
		stateCode = STATE_CODE_ANKAN;
	}

	// 魑ｴ縺�◆縺薙→縺ｨ縺昴�迚後ｒ蜷��繝ｬ繧､繝､繝ｼ縺ｫ莨昴∴繧�
	private void notifyNaki(Player player, Mentu mentu) {
		for (Kaze kaze : transporterMap.keySet()) {
			transporterMap.get(kaze).notifyNaki(player, mentu);
		}
	}

	// 證玲ｧ薙＠縺溘�繝ｬ繧､繝､繝ｼ縺ｨ縺昴�迚後ｒ蜷��繝ｬ繧､繝､繝ｼ縺ｫ莨昴∴繧�
	private void doAnkan() {

		if (kyoku.isAnkanable()) {
			Player p = kyoku.getCurrentPlayer();
			if (p.isMan()) {
				Transporter tr = transporterMap.get(kyoku.getCurrentTurn());
				List<Integer> ankanlist = waitAnkan(tr,
						kyoku.getAnkanableHaiList());
				if (ankanlist != null) {
					Mentu ankanMentu = kyoku.doAnkan(ankanlist);
					System.out.println(ankanMentu);
					notifyNaki(p, ankanMentu);

					stateCode = STATE_CODE_RINSYANTSUMO;
					return;
				}
			} else {
				// AI
				AI ai = aiMap.get(kyoku.getCurrentTurn());
				if (ai.ankan(kyoku.getAnkanableHaiList()) != -1) {
					Mentu ankanMentu = kyoku.doAnkan(kyoku
							.getAnkanableHaiList().get(0));
					notifyNaki(p, ankanMentu);
					stateCode = STATE_CODE_RINSYANTSUMO;
					return;
				}
			}
		}
		stateCode = STATE_CODE_ISREACH;
		// to be defined
	}

	// 繝ｪ繝ｼ繝√＠縺ｦ縺�ｋ縺ｨ縺阪�繝�Δ蛻�ｊ,繝ｪ繝ｼ繝√〒縺阪ｋ縺ｨ縺阪�wait繧貞他縺ｳ蜃ｺ縺吶�
	private void reach() {
		Player p = kyoku.getCurrentPlayer();
		Transporter tr = transporterMap.get(kyoku.getCurrentTurn());
		if (p.isMan()) {
			if (kyoku.isReach(kyoku.getCurrentTurn())) {
				try {
					Thread.sleep(1000);
					kyoku.discardTsumoHai();
					sendTsumoGiri(tr);

					System.out.println("現在捨て牌:" + kyoku.getCurrentSutehai());
					Console.wairEnter();

					stateCode = STATE_CODE_RON;
					return;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				if (kyoku.isReachable()) {
					int reach = waitReach(tr, kyoku.getReachableHaiList());
					if (reach != -1) {
						kyoku.doReach();
						kyoku.discard(reach);

						System.out
								.println("迴ｾ蝨ｨ謐ｨ縺ｦ迚鯉ｼ�" + kyoku.getCurrentSutehai());
						Console.wairEnter();

						for (Kaze kaze : transporterMap.keySet()) {
							Server treach = transporterMap.get(kaze);

							int reachSutehaiIndex = kyoku.getSutehaiList(
									kyoku.getCurrentTurn()).size() - 1;
							for (int i = 0; i < kyoku.getSutehaiList(
									kyoku.getCurrentTurn()).size(); i++) {
								if (kyoku
										.getSutehaiList(kyoku.getCurrentTurn())
										.get(i).isNaki()) {
									reachSutehaiIndex--;
								}
							}
							notifyReach(kyoku.getCurrentTurn(),
									reachSutehaiIndex, treach);
						}
						stateCode = STATE_CODE_RON;
					} else {
						stateCode = STATE_CODE_DISCARD;
					}
				} else {
					stateCode = STATE_CODE_DISCARD;
				}
			}
		} else {
			// AI縺ｮ蜃ｦ逅�
			if (kyoku.isReach(kyoku.getCurrentTurn())) {
				// try {
				// Thread.sleep(1000);
				kyoku.discardTsumoHai();
				System.out.println("迴ｾ蝨ｨ謐ｨ縺ｦ迚鯉ｼ�" + kyoku.getCurrentSutehai());
				Console.wairEnter();

				stateCode = STATE_CODE_RON;
				return;
				// } catch (InterruptedException e) {
				// e.printStackTrace();
				// }
			}
			AI ai = aiMap.get(kyoku.getCurrentTurn());

			if (kyoku.isReachable() && ai.isReach()) {
				kyoku.doReach();
				int index = ai.discard();
				kyoku.discard(index);
				int reachSutehaiIndex = kyoku.getSutehaiList(
						kyoku.getCurrentTurn()).size() - 1;
				for (int i = 0; i < kyoku
						.getSutehaiList(kyoku.getCurrentTurn()).size(); i++) {
					if (kyoku.getSutehaiList(kyoku.getCurrentTurn()).get(i)
							.isNaki()) {
						reachSutehaiIndex--;
					}
				}
				for (Transporter t : transporterMap.values()) {
					t.notifyReach(kyoku.getCurrentTurn(), reachSutehaiIndex);
				}
				System.out.println("迴ｾ蝨ｨ謐ｨ縺ｦ迚鯉ｼ�" + kyoku.getCurrentSutehai());
				Console.wairEnter();
				stateCode = STATE_CODE_RON;
				return;
			} else {
				stateCode = STATE_CODE_DISCARD;
				return;
			}
			// AI
		}
	}

	// 謐ｨ縺ｦ繧狗煙縺後け繝ｩ繧､繧｢繝ｳ繝医°繧蛾�繧峨ｌ縺ｦ縺上ｋ縺ｮ繧貞ｾ�▽wait繧貞他縺ｳ蜃ｺ縺�繝�Δ蛻�ｊ縺区焔蜃ｺ縺励°繧貞玄蛻･縺励※謐ｨ縺ｦ繧九�
	private void discard() {
		Player p = kyoku.getCurrentPlayer();
		if (p.isMan()) {
			Transporter tr = transporterMap.get(kyoku.getCurrentTurn());
			int i = waitDiscarded(tr, isThereTsumohai(tr));

			if (0 <= i && i <= 12) {
				kyoku.discard(i);
				afterDiscard();
			} else if (i == 13) {
				kyoku.discard(13);
				afterDiscard();

			} else {
				System.out.println("荳肴ｭ｣縺ｪ蜈･蜉帙〒縺�");
			}
		} else {
			AI ai = aiMap.get(kyoku.getCurrentTurn());
			kyoku.discard(ai.discard());
			afterDiscard();
			// AI
		}
	}

	// 謐ｨ縺ｦ繧峨ｌ縺溽煙縺ｨ縺昴�繝励Ξ繧､繝､繝ｼ繧貞推繝励Ξ繧､繝､繝ｼ縺ｫ莨昴∴繧九�
	private void afterDiscard() {
		System.out.println("迴ｾ蝨ｨ謐ｨ縺ｦ迚鯉ｼ�" + kyoku.getCurrentSutehai());
		Console.wairEnter();
		for (Kaze kaze : transporterMap.keySet()) {
			Server tnotify = transporterMap.get(kaze);
			notifyDiscard(kyoku.getCurrentPlayer(), kyoku.getCurrentSutehai(),
					false, tnotify);
		}
	}

	// 繝ｭ繝ｳ縺ゅ′繧翫〒縺阪ｋ縺ｨ縺阪↓繝励Ξ繧､繝､繝ｼ縺ｫ繝ｭ繝ｳ縺吶ｋ縺九←縺�°繧定◇縺�
	private void sendRonAgari() {
		for (Kaze kaze : Kaze.values()) {
			Player p = playerMap.get(kaze);
			if (p.isMan()) {
				Server tr = transporterMap.get(kaze);
				if (kyoku.isRonable(kaze))
					tr.requestRon();
			}
		}
	}

	// 繝ｭ繝ｳ荳翫′繧翫〒縺阪ｋ縺ｨ騾√▲縺溷ｾ�縺昴�蝗樒ｭ斐′騾√ｉ繧後※縺上ｋ縺ｮ繧貞ｾ�▽
	private void doRon(List<Player> doRonPlayer) {

		for (Kaze kaze : Kaze.values()) {
			if (kyoku.isRonable(kaze)) {
				Player p = playerMap.get(kaze);
				if (p.isMan()) {
					Transporter tr = transporterMap.get(kaze);
					boolean doRon = waitRon(tr);
					if (doRon) {
						kyoku.doRon(kaze);
						doRonPlayer.add(p);

						Map<Player, List<Hai>> map = new HashMap<Player, List<Hai>>();
						for (int i = 0; i < kyoku.getTehaiList(kaze).size(); i++) {
							map.put(p, kyoku.getTehaiList(kaze));
						}

						for (Kaze ronKaze : transporterMap.keySet()) {
							Server tron = transporterMap.get(ronKaze);
							notifyRon(map, tron);
							System.out.println(playerMap.get(ronKaze)
									+ " : 繝ｭ繝ｳ縺�ｼ�");
							Console.wairEnter();
						}

						if (kyoku.isSanchaho()) {
							kyoku.doTotyuRyukyokuSanchaho();
						}
						kyoku.doSyukyoku();
					} else {
						kyoku.onRonRejected(kaze);
					}
				} else {
					// AI
					AI ai = aiMap.get(kaze);
					if (ai.isRon()) {
						kyoku.doRon(kaze);
						Map<Player, List<Hai>> map = new HashMap<Player, List<Hai>>();
						for (int i = 0; i < kyoku.getTehaiList(kaze).size(); i++) {
							map.put(p, kyoku.getTehaiList(kaze));
						}
						doRonPlayer.add(p);

						for (Kaze ronKaze : transporterMap.keySet()) {
							Server tron = transporterMap.get(ronKaze);
							notifyRon(map, tron);
							System.out.println(playerMap.get(ronKaze)
									+ " : 繝ｭ繝ｳ縺�ｼ�");
							Console.wairEnter();
						}

						if (kyoku.isSanchaho()) {
							kyoku.doTotyuRyukyokuSanchaho();
						}
						kyoku.doSyukyoku();
					} else {
						kyoku.onRonRejected(kaze);
					}
				}
			}

		}

	}

	// 謐ｨ縺ｦ迚後°繧峨Ο繝ｳ縺ゅ′繧翫☆繧九�
	private void doRonAgari() {
		List<Player> doRonPlayer = new ArrayList<Player>();
		doRon(doRonPlayer);
		if (doRonPlayer.size() == 0) {
			stateCode = STATE_CODE_SUCHA;
		} else {
			stateCode = STATE_CODE_ENDOFKYOKU;
		}
	}

	// 蜉�ｧ薙＠縺溽煙縺九ｉ繝ｭ繝ｳ縺ゅ′繧翫☆繧九�
	private void doChankanRonAgari() {
		List<Player> doRonPlayer = new ArrayList<Player>();
		doRon(doRonPlayer);
		if (doRonPlayer.size() == 0) {
			stateCode = STATE_CODE_RINSYANTSUMO;
		} else {
			stateCode = STATE_CODE_ENDOFKYOKU;
		}
	}

	// 蝗帛ｮｶ遶狗峩,蝗帛ｮｶ騾｣謇�蝗幃幕讒薙�蛻､螳�
	private void isSucha(Kyoku k) {
		if (kyoku.isSuchaReach() || kyoku.isSufontsuRenta()
				|| kyoku.isSukaikan()) {
			kyoku.doSuchaReach();
			stateCode = STATE_CODE_ENDOFKYOKU;
		} else {
			stateCode = STATE_CODE_NAKI;
		}
	}

	// 譏取ｧ薙〒縺阪ｋ縺ｨ縺阪↓,縺昴�繝励Ξ繧､繝､繝ｼ縺ｫ譏取ｧ薙〒縺阪ｋ縺ｨ縺�≧縺薙→繧剃ｼ昴∴繧九�
	private void sendMinkan() {
		for (Kaze kaze : Kaze.values()) {
			if (kyoku.isMinkanable(kaze)) {
				Player p = playerMap.get(kaze);
				if (p.isMan()) {
					Server tr = transporterMap.get(kaze);
					sendMinkanableIndexList(tr, kyoku.getMinkanableList(kaze));
				} else {
					// AI
				}
			}
		}
	}

	// 譏取ｧ薙☆繧九→霑斐▲縺ｦ縺阪◆繧画�讒薙☆繧九�
	private void doMinkan() {
		for (Kaze kaze : Kaze.values()) {
			if (kyoku.isMinkanable(kaze)) {
				Player p = playerMap.get(kaze);
				if (p.isMan()) {
					Transporter tr = transporterMap.get(kaze);
					boolean isMinkanDo = waitMinkan(tr,
							kyoku.getMinkanableList(kaze));
					if (isMinkanDo) {
						Mentu minkanMentu = kyoku.doMinkan(kaze);
						notifyNaki(p, minkanMentu);

						System.out.println(kyoku.getPlayer(kaze) + " : 譏取ｧ薙＠縺ｾ縺�");
						Console.wairEnter();

						stateCode = STATE_CODE_RINSYANTSUMO;
						return;
					}
				} else {
					AIType01 ai = new AIType01(p);
					if (ai.minkan()) {
						Mentu minkanMentu = kyoku.doMinkan(kaze);
						notifyNaki(p, minkanMentu);

						System.out.println(kyoku.getPlayer(kaze) + " : 譏取ｧ薙＠縺ｾ縺�");
						Console.wairEnter();

						stateCode = STATE_CODE_RINSYANTSUMO;
					}
				}
			}
		}

	}

	// 繝昴Φ縺ｧ縺阪ｋ縺ｨ縺阪↓繝昴Φ縺ｧ縺阪ｋ縺ｨ縺�≧縺薙→繧偵◎縺ｮ繝励Ξ繧､繝､繝ｼ縺ｫ莨昴∴繧九�
	private void sendPon() {
		for (Kaze kaze : Kaze.values()) {
			if (kyoku.isPonable(kaze)) {
				Player p = playerMap.get(kaze);
				if (p.isMan()) {
					Server tr = transporterMap.get(kaze);
					sendPonanbleIndexLists(tr, kyoku.getPonableHaiList(kaze));
				} else {

				}
			}
		}
	}

	// 繝昴Φ縺吶ｋ縺ｨ霑斐▲縺ｦ縺阪◆繧�繝昴Φ縺吶ｋ縲�
	private void doPon() {
		for (Kaze kaze : Kaze.values()) {
			if (kyoku.isPonable(kaze)) {
				Player p = playerMap.get(kaze);
				if (p.isMan()) {
					Transporter tr = transporterMap.get(kaze);
					List<Integer> ponlist = waitPon(tr);
					if (ponlist != null) {
						Mentu ponMentu = kyoku.doPon(kaze, ponlist);
						notifyNaki(p, ponMentu);

						System.out.println(kyoku.getPlayer(kaze) + " : 繝昴Φ��");
						Console.wairEnter();

						stateCode = STATE_CODE_DISCARD;
						return;
					}
				} else {
					// AI
					AI ai = aiMap.get(kaze);
					if (ai.pon(kyoku.getPonableHaiList(kaze)) != -1) {
						Mentu ponMentu = kyoku.doPon(
								kaze,
								kyoku.getPonableHaiList(kaze).get(
										ai.pon(kyoku.getPonableHaiList(kaze))));
						notifyNaki(p, ponMentu);

						System.out.println(kyoku.getPlayer(kaze) + " : 繝昴Φ��");
						Console.wairEnter();

						stateCode = STATE_CODE_DISCARD;
						return;

					}

				}
			}
		}
	}

	// 繝√�縺ｧ縺阪ｋ繝励Ξ繧､繝､繝ｼ縺ｫ繝√�縺ｧ縺阪ｋ縺ｨ縺�≧縺薙→繧剃ｼ昴∴繧九�
	private void sendChi() {
		if (kyoku.isChiable()) {
			Player p = playerMap.get(kyoku.getCurrentTurn().simo());
			if (p.isMan()) {
				Server tr = transporterMap.get(kyoku.getCurrentTurn().simo());
				sendChiableIndexLists(tr, kyoku.getChiableHaiList());
			} else {
				// AI
			}
		}
	}

	// 繝√�縺吶ｋ縺ｨ霑斐▲縺ｦ縺阪◆縺ｨ縺�繝√�縺吶ｋ
	private void doChi() {
		if (kyoku.isChiable()) {
			Player p = playerMap.get(kyoku.getCurrentTurn().simo());
			if (p.isMan()) {
				Transporter tr = transporterMap.get(kyoku.getCurrentTurn()
						.simo());
				System.out.println(tr.isChiReceived());

				List<Integer> chilist = waitChi(tr, kyoku.getChiableHaiList());
				System.out.println("SystemThreadReceived: ");
				if (chilist != null) {
					Mentu chiMentu = kyoku.doChi(chilist);
					notifyNaki(p, chiMentu);

					System.out.println(kyoku.getPlayer(kyoku.getCurrentTurn()
							.simo()) + " : 繝√�縺�●");
					Console.wairEnter();

					stateCode = STATE_CODE_DISCARD;
					return;
				}

			} else {
				// AI
				AI ai = aiMap.get(kyoku.getCurrentTurn().simo());
				int index = -1;
				if ((index = ai.chi(kyoku.getChiableHaiList())) != -1) {
					Mentu chiMentu = kyoku.doChi(kyoku.getChiableHaiList().get(
							index));
					notifyNaki(p, chiMentu);

					System.out.println(kyoku.getPlayer(kyoku.getCurrentTurn()
							.simo()) + " : 繝√�縺�●");
					Console.wairEnter();

					stateCode = STATE_CODE_DISCARD;
					return;
				}
			}

		}
	}

	// 蜈ｨ縺ｦ縺ｮ繝励Ξ繧､繝､繝ｼ縺ｮ繝医Λ繝ｳ繧ｹ繝昴�繧ｿ縺ｮ繝輔Λ繧ｰ遲峨ｒ蜈�↓謌ｻ縺吶�
	private void initTransporterFlag() {
		for (Transporter tr : transporterMap.values()) {
			tr.init();
		}
	}
	
	//discardFlag繧貞�譛溷喧縺ｫ縺吶ｋ
	private void initDiscardFlag(){
		discardFlag =false;
	}

	//discardFlag繧弛n縺ｫ縺吶ｋ
	private void onDiscardFlag(){
		discardFlag = true;
	}
	
	/*
	 * **********************A()******************************
	 */

	// requestKyusyukyuhai()縺碁�繧峨ｌ縺溘→縺�縺昴�霑皮ｭ斐′霑斐▲縺ｦ縺上ｋ縺ｾ縺ｧ蠕�▽
	private boolean waitKyusyukyuhai(Transporter tr) {
		// tr.requestKyusyukyuhai();
		while (!tr.isKyusyukyuhaiReceived() && !tr.getGrandFlag()
				&& !tr.isDiscardedReceived()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return tr.getKyusyukyuhaiResult();
	}

	// 繝�Δ縺励※縺九ｉ迚後′蛻�ｉ繧後ｋ縺ｮ縺�繝�Δ縺帙★縺ｫ迚後′蛻�ｉ繧後ｋ(繝昴Φ,繝√�)縺ｮ縺�
	private boolean isThereTsumohai(Transporter tr) {
		return tr.isThereTsumohai();
	}

	// 謐ｨ縺ｦ迚後ｒ驕ｸ縺ｹ縺ｨ縺�≧蜻ｽ莉､繧帝�縺｣縺ｦ,蠕�▽
	private int waitDiscarded(Transporter tr, boolean tumoari) {
		if(discardFlag)
			tr.sendDiscard(tumoari);
		
		while (!tr.isDiscardedReceived()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return tr.getDiscardedIndex();
	}

	private void sendTsumoGiri(Server tr) {
		tr.sendTsumoGiri();
	}

	// 繝√�縺ｧ縺阪ｋ迚後�繝ｪ繧ｹ繝医ｒ騾√ｋ縲�
	private void sendChiableIndexLists(Server tr, List<List<Integer>> list) {
		tr.sendChiableIndexLists(list);
	}

	// 繝√�縺ｧ縺阪ｋ縺ｨ騾√▲縺溷ｾ�縺昴�蝗樒ｭ斐′霑斐▲縺ｦ縺上ｋ縺ｾ縺ｧ蠕�▽縲�
	private List<Integer> waitChi(Transporter tr, List<List<Integer>> sendlist) {
		while (!tr.isChiReceived() && !tr.isPonReceived()
				&& !tr.isMinkanReceived()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return tr.getChiIndexList();
	}

	// 繝昴Φ縺ｧ縺阪ｋ縺ｨ騾√▲縺溷ｾ�縺昴�蝗樒ｭ斐′霑斐▲縺ｦ縺上ｋ縺ｾ縺ｧ蠕�▽縲�
	private List<Integer> waitPon(Transporter tr) {
		while (!tr.isPonReceived() && !tr.isChiReceived()
				&& !tr.isMinkanReceived()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return tr.getPonIndexList();
	}

	// 繝昴Φ縺ｧ縺阪ｋ縺ｨ縺�縺昴�繝昴Φ縺ｧ縺阪ｋ迚後�繝ｪ繧ｹ繝医ｒ騾√ｋ
	private void sendPonanbleIndexLists(Server tr, List<List<Integer>> list) {
		tr.sendPonableIndexLists(list);
	}

	// 證玲ｧ薙〒縺阪ｋ縺ｨ縺�≧縺薙→繧帝�縺｣縺溷ｾ�縺昴�蝗樒ｭ斐ｒ蠕�▽
	private List<Integer> waitAnkan(Transporter tr, List<List<Integer>> sendlist) {
		while (!tr.isAnkanReceived() && !tr.getGrandFlag()
				&& !tr.isDiscardedReceived()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return tr.getAnkanIndexList();

	}

	// 譏取ｧ薙〒縺阪ｋ縺ｨ騾√▲縺溘≠縺ｨ,縺昴�蝗樒ｭ斐ｒ蠕�▽
	private boolean waitMinkan(Transporter tr, List<Integer> sendIndexList) {
		while (!tr.isMinkanReceived() && !tr.isChiReceived()
				&& !tr.isPonReceived()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return tr.isMinkanDo();
	}

	// 譏取ｧ薙〒縺阪ｋ迚後�繝ｪ繧ｹ繝医ｒ騾√ｋ
	private void sendMinkanableIndexList(Server tr, List<Integer> sendIndexList) {
		tr.sendMinkanableIndexList(sendIndexList);
	}

	// 蜉�ｧ薙〒縺阪ｋ縺ｨ騾√▲縺溷ｾ�縺昴�蝗樒ｭ斐′霑斐▲縺ｦ縺上ｋ縺ｮ繧貞ｾ�▽縲�
	private int waitKakan(Transporter tr, List<Integer> sendKakanList) {
		while (!tr.isKakanReceived() && !tr.getGrandFlag()
				&& !tr.isDiscardedReceived()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return tr.getKakanindex();
	}

	// 繝ｪ繝ｼ繝√〒縺阪ｋ縺ｨ縺�繝ｪ繝ｼ繝√☆繧九°縺ｩ縺�°縺ｮ蝗樒ｭ斐′霑斐▲縺ｦ縺上ｋ縺ｾ縺ｧ蠕�▽縲�
	private int waitReach(Transporter tr, List<Integer> sendReachableList) {
		while (!tr.isReachReceived() && !tr.getGrandFlag()
				&& !tr.isDiscardedReceived()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return tr.getReachHaiIndex();
	}

	// 繝ｭ繝ｳ荳翫′繧翫〒縺阪ｋ縺ｨ騾√▲縺溷ｾ�縺昴�蝗樒ｭ斐′霑斐▲縺ｦ縺上ｋ縺ｮ繧貞ｾ�▽
	private boolean waitRon(Transporter tr) {
		while (!tr.isRonReceived()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return tr.isRonDo();
	}

	// 繝�Δ荳翫′繧翫〒縺阪ｋ縺ｨ騾√▲縺溷ｾ�縺昴�蝗樒ｭ斐′霑斐▲縺ｦ縺上ｋ縺ｮ繧貞ｾ�▽
	private boolean waitTsumoagari(Transporter tr) {
		while (!tr.isTsumoagariDo() && !tr.isDiscardedReceived()
				&& !tr.getGrandFlag()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return tr.isTsumoagariDo();
	}

	// 蛻�▲縺溽煙縺ｨ縺昴�繝励Ξ繧､繝､繝ｼ繧貞推繝励Ξ繧､繝､繝ｼ縺ｫ莨昴∴繧�
	private void notifyDiscard(Player player, Hai hai, boolean tumokiri,
			Server tr) {
		tr.notifyDiscard(player, hai, tumokiri);
	}

	// 繝ｭ繝ｳ縺励◆繝励Ξ繧､繝､繝ｼ縺ｮ繝ｪ繧ｹ繝医→縺昴�縺ゅ′縺｣縺滓焔迚後ｒ蜷��繝ｬ繧､繝､繝ｼ縺ｫ騾√ｋ縲�
	private void notifyRon(Map<Player, List<Hai>> map, Server tr) {
		tr.notifyRon(map);
	}

	//繝ｪ繝ｼ繝√＠縺溘�繝ｬ繧､繝､繝ｼ縺ｨ菴輔�迚後〒繝ｪ繝ｼ繝√＠縺溘°繧貞推繝励Ξ繧､繝､繝ｼ縺ｫ騾√ｋ縲�
	private void notifyReach(Kaze currentTurn, int sutehaiIndex, Server tr) {
		tr.notifyReach(currentTurn, sutehaiIndex);
	}

	//閨ｴ迚後＠縺溘�繝ｬ繧､繝､繝ｼ縺ｮ謇狗煙繧貞捉繧翫↓隕九○繧�
	@SuppressWarnings("null")
	private void doTempai(){
		Map<Player,List<Hai>> tempaiMap = null;
		Server ttempai = null;
		for (Kaze kaze : Kaze.values()){
			if(kyoku.isTenpai(kaze)){
				tempaiMap.put(kyoku.getPlayer(kaze),kyoku.getTehaiList(kaze));
			}
			ttempai = transporterMap.get(kaze);
		}
		notifyTempai(tempaiMap,ttempai);
	}
	
	//閨ｴ迚後＠縺溘％縺ｨ繧貞捉繧翫↓遏･繧峨○繧九�
	private void notifyTempai(Map<Player,List<Hai>> tehaimap,Server tr){
		tr.notifyTempai(tehaimap);		
	}
	
	public static final int STATE_CODE_TSUMO = 0;
	public static final int STATE_CODE_KYUSYUKYUHAI = 1;
	public static final int STATE_CODE_TSUMOAGARI = 2;
	public static final int STATE_CODE_KAKAN = 3;
	public static final int STATE_CODE_CHANKANRON = 4;
	public static final int STATE_CODE_RINSYANTSUMO = 5;
	public static final int STATE_CODE_ANKAN = 6;
	public static final int STATE_CODE_ISREACH = 7;
	public static final int STATE_CODE_DISCARD = 8;
	public static final int STATE_CODE_SUCHA = 9;
	public static final int STATE_CODE_NAKI = 10;
	public static final int STATE_CODE_RON = 11;
	public static final int STATE_CODE_SEND = 12;
	public static final int STATE_CODE_NEXTTURN = 13;
	public static final int STATE_CODE_ENDOFKYOKU = 14;

}
