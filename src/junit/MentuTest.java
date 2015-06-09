package junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static system.MajanHai.ITI_MAN;
import static system.MajanHai.ITI_SOU;
import static system.MajanHai.NI_MAN;
import static system.MajanHai.NI_SOU;
import static system.MajanHai.SAN_MAN;
import static system.MajanHai.SAN_SOU;
import static system.MajanHai.TON;

import org.junit.Test;

import system.Hai;
import system.Kaze;
import system.MajanHai;
import system.Mentu;

public class MentuTest {
	@Test
	public void testCopy() {
		Mentu m1 = new Mentu(SAN_MAN, ITI_MAN, NI_MAN);
		Mentu m2 = new Mentu(m1, Kaze.TON, NI_MAN);
		assertEquals(m2.isNaki(), true);
		assertEquals(m2.getKaze(), Kaze.TON);
		assertTrue(m2.get(1).isNaki());
	}

	@Test
	public void testOrder() {
		Mentu m1 = new Mentu(SAN_MAN, ITI_MAN, NI_MAN);
		assertEquals(m1.type(), Mentu.Type.SYUNTU);
		assertEquals(m1.get(0).type(), ITI_MAN.type());
		assertEquals(m1.get(1).type(), NI_MAN.type());
		assertEquals(m1.get(2).type(), SAN_MAN.type());
	}

	@Test
	public void testAnko() {
		Mentu m1 = new Mentu(ITI_MAN, ITI_MAN, ITI_MAN);
		assertEquals(m1.type(), Mentu.Type.KOTU);
		assertEquals(m1.calcHu(), 8);
		assertEquals(m1.size(), 3);
		assertEquals(m1.isKakan(), false);
		assertEquals(m1.isNaki(), false);
		assertEquals(m1.getKaze(), null);

		Mentu m2 = new Mentu(TON, TON, TON);
		assertEquals(m2.type(), Mentu.Type.KOTU);
		assertEquals(m2.calcHu(), 8);
		assertEquals(m2.size(), 3);
		assertEquals(m2.isKakan(), false);
		assertEquals(m2.isNaki(), false);
		assertEquals(m2.getKaze(), null);
	}

	@Test
	public void testEquals() {
		Mentu m1 = new Mentu(ITI_MAN, ITI_MAN, ITI_MAN);
		Mentu m2 = new Mentu(TON, TON, TON);

		Mentu m3 = new Mentu(ITI_SOU, NI_SOU, SAN_SOU);
		Mentu m4 = new Mentu(ITI_SOU, NI_SOU, SAN_SOU);
		Mentu m5 = new Mentu(ITI_SOU, Kaze.TON, NI_SOU, SAN_SOU);

		assertFalse(m1.equals(m2));
		assertTrue(m1.equals(m1));
		assertTrue(m3.equals(m4));
		assertFalse(m3.equals(m5));
	}

	@Test
	public void testKakan() {
		Mentu m1 = new Mentu(ITI_MAN, Kaze.TON, ITI_MAN, ITI_MAN);
		Mentu m2 = m1.doKakan(ITI_MAN);
		assertTrue(m2.isKakan());
		assertTrue(m2.isNaki());
		assertTrue(m2.size() == 4);
	}

	@Test
	public void testConstruct() {
		for (Hai hai1 : MajanHai.values()) {
			for (Hai hai2 : MajanHai.values()) {
				for (Hai hai3 : MajanHai.values()) {
					Mentu m = null;
					try {
						m = new Mentu(hai1, hai2, hai3);
					} catch (IllegalArgumentException e) {
						assertEquals(m, null);
					}
					if (m != null) {
						assertTrue(m.type() != null);
					}
				}
			}
		}
	}
}
