package system;

import java.util.List;

import system.Mentu.Type;

/**
 * 待ちの種類を表す。例えば,両面待ち,単騎待ちなどがある。
 */
public enum MatiType {
	TANKI(2) {
		@Override
		public boolean check(HaiType agari, Mentu m, HaiType janto) {
			if (janto == agari)
				return true;
			return false;
		}

		@Override
		public boolean check(List<Mentu> m, HaiType agari, HaiType janto) {
			if (janto == agari)
				return true;
			return false;
		}
	},
	RYANMEN(0) {
		@Override
		public boolean check(HaiType agari, Mentu m, HaiType janto) {

			// あがり牌が数牌でない場合
			if (agari.isTsuhai())
				return false;

			if (m.type() == Type.SYUNTU) {
				if (agari.suType() != m.get(0).type().suType())
					return false;

				if (m.get(0).type().number() != 7 && m.get(0).type() == agari)
					return true;
				if (m.get(2).type().number() != 3 && m.get(2).type() == agari)
					return true;
			}
			return false;
		}

		@Override
		public boolean check(List<Mentu> mlist, HaiType agari, HaiType janto) {
			// あがり牌が数牌でない場合
			if (agari.isTsuhai())
				return false;

			for (Mentu m : mlist) {
				if (m.type() == Type.SYUNTU) {
					if (agari.suType() != m.get(0).type().suType())
						continue;

					if (m.get(0).type().number() != 7 && m.get(0).type() == agari)
						return true;
					if (m.get(2).type().number() != 3 && m.get(2).type() == agari)
						return true;
				}
			}
			return false;
		}
	},
	KANTYAN(2) {
		@Override
		public boolean check(HaiType agari, Mentu m, HaiType janto) {
			// あがり牌が数牌でない場合
			if (agari.isTsuhai())
				return false;

			if (m.type() == Type.SYUNTU) {
				if (agari.suType() != m.get(0).type().suType())
					return false;

				if (m.get(1).type() == agari)
					return true;
			}
			return false;
		}

		@Override
		public boolean check(List<Mentu> mlist, HaiType agari, HaiType janto) {
			// あがり牌が数牌でない場合
			if (agari.isTsuhai())
				return false;

			for (Mentu m : mlist) {
				if (m.type() == Type.SYUNTU) {
					if (agari.suType() != m.get(0).type().suType())
						continue;

					if (m.get(1).type() == agari)
						return true;
				}
			}
			return false;
		}
	},
	PENTYAN(2) {
		@Override
		public boolean check(HaiType agari, Mentu m, HaiType janto) {
			// あがり牌が数牌でない場合
			if (agari.isTsuhai())
				return false;

			int number = agari.number();
			if (number != 3 && number != 7)
				return false;

			if (m.type() == Type.SYUNTU) {
				if (agari.suType() != m.get(0).type().suType())
					return false;

				if (m.get(0).type().number() == 7)
					return true;
				if (m.get(2).type().number() == 3)
					return true;
			}
			return false;
		}

		@Override
		public boolean check(List<Mentu> mlist, HaiType agari, HaiType janto) {
			// あがり牌が数牌でない場合
			if (agari.isTsuhai())
				return false;

			int number = agari.number();
			if (number != 3 && number != 7)
				return false;

			for (Mentu m : mlist) {
				if (m.type() == Type.SYUNTU) {
					if (agari.suType() != m.get(0).type().suType())
						continue;

					if (m.get(0).type().number() == 7)
						return true;
					if (m.get(2).type().number() == 3)
						return true;
				}
			}
			return false;
		}
	},
	SYABO(0) {
		@Override
		public boolean check(HaiType agari, Mentu m, HaiType janto) {
			if (m.type() != Type.KOTU)
				return false;

			if (m.get(0).type() == agari)
				return true;
			return false;
		}

		@Override
		public boolean check(List<Mentu> mlist, HaiType agari, HaiType janto) {
			for (Mentu m : mlist) {
				if (m.type() != Type.KOTU)
					continue;
				if (m.get(0).type() == agari)
					return true;
			}
			return false;
		}
	};

	private int hu;

	private MatiType(int hu) {
		this.hu = hu;
	}

	/**
	 * この待ちタイプの符を返す。
	 * @return 符
	 */
	public int hu() {
		return this.hu;
	}

	/**
	 * 指定された面子とあがり牌からこの待ちの種類であるかどうかを判定する．
	 * @param agari あがり牌の種類．
	 * @param m 待ちの種類を調べる面子．単騎を判定する場合はnullでよい．
	 * @param janto 雀頭の牌の種類．
	 * @return
	 */
	public abstract boolean check(HaiType agari, Mentu m, HaiType janto);

	public abstract boolean check(List<Mentu> mlist, HaiType agari, HaiType janto);

	public static MatiType getMatiType(HaiType agari, Mentu m, HaiType janto) {
		for (MatiType mt : values()) {
			if (mt.check(agari, m, janto)) {
				// exclude the case of TANKI
				if(mt == TANKI)
					continue;
				return mt;
			}
		}
		return null;
	}
	
	public static boolean isTanki(HaiType agari, HaiType janto) {
		return TANKI.check(null, agari, janto);
	}

}
