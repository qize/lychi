package lychi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import chemaxon.formats.MolFormatException;
import chemaxon.struc.MolAtom;
import chemaxon.struc.MolBond;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;

@RunWith(Parameterized.class)
public class LychiRegressionTest {
	
	public static class LychiTestInstance{
		String name;
		String input;
		String expectedLychi;
		boolean shouldMatch=true;
		
		
		public static LychiTestInstance of(String smi, String lychi){
			LychiTestInstance ltest= new LychiTestInstance();
			ltest.input=smi;
			ltest.expectedLychi=lychi;
			ltest.name=smi;
			return ltest;
		}
		
		public static LychiTestInstance equivalent(String smi1, String smi2){
			try{
				MolHandler mh = new MolHandler();
				mh.setMolecule(smi2);
				Molecule m= mh.getMolecule();
				LyChIStandardizer std = new LyChIStandardizer();
				std.standardize(m);
				String fullKey=LyChIStandardizer.hashKey(m);
				return of(smi1,fullKey);
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}
		public static LychiTestInstance notEquivalent(String smi1, String smi2){
			return equivalent(smi1,smi2).negate();
		}
		public LychiTestInstance negate(){
			this.shouldMatch=!this.shouldMatch;
			return this;
		}
		
		public LychiTestInstance name(String n){
			this.name=n;
			return this;
		}
		
		
		public Object[] asJunitInput(){
			return new Object[]{this.name, this};
		}
		
		public Molecule getMolecule() throws MolFormatException{
			MolHandler mh = new MolHandler();
			mh.setMolecule(input);
			return mh.getMolecule();
			
		}
	}
	
	
	

	private LychiTestInstance spec;

	public LychiRegressionTest(String ignored, LychiTestInstance spec){
		this.spec = spec;
	}
	
	public static void basicTest(Molecule m, String expected, boolean match) throws Exception{
		LyChIStandardizer std = new LyChIStandardizer();
		std.standardize(m);
		String fullKey=LyChIStandardizer.hashKey(m);
		if(match){
			assertEquals(expected,fullKey);
		}else{
			assertNotEquals(expected,fullKey);
		}
	}
	
	public static Molecule shuffleMolecule(Molecule m, int[] map){
		MolAtom[] mas=m.getAtomArray();
		MolBond[] mbs=m.getBondArray();
		
		Molecule m2=new Molecule();
		
		int[] rmap = new int[map.length];
		
		for(int i=0;i<map.length;i++){
			m2.add((MolAtom)mas[map[i]].clone());
			rmap[map[i]]=i;
		}
		MolAtom[] nmas = m2.getAtomArray();
		for(int j=0;j<mbs.length;j++){
			MolBond mb = mbs[j];
			int oi1=m.indexOf(mb.getAtom1());
			int oi2=m.indexOf(mb.getAtom2());
			int ni1=rmap[oi1];
			int ni2=rmap[oi2];
			MolBond nmb=mb.cloneBond(nmas[ni1], nmas[ni2]);
			m2.add(nmb);
		}
		
		return m2;
		
	}
	
	@Test
	public void correctLychiFirstTime() throws Exception{
		basicTest(spec.getMolecule(),spec.expectedLychi, spec.shouldMatch);
	}
	
	@Test
	public void correctLychiAfter10Times() throws Exception{
		for (int i=0;i<10;i++){
			correctLychiFirstTime();
		}
	}
	
	@Test
	public void correctLychiAfterRandomShuffle() throws Exception{
		Molecule m=spec.getMolecule();
		m.clean(2, null);
		Random r= new Random(0xFAB5EED);
		for (int i=0;i<10;i++){
			
			List<Integer> iatoms=IntStream.range(0, m.getAtomCount()).mapToObj(i2->i2).collect(Collectors.toList());
			Collections.shuffle(iatoms, r);
			int[] map =iatoms.stream().mapToInt(i1->i1).toArray();
			Molecule s=shuffleMolecule(m,map);
			
			basicTest(s,spec.expectedLychi,spec.shouldMatch);
		}
	}
	
	//@Test
	public void daisyChainLychiAfter10Times() throws Exception{
		Molecule m=spec.getMolecule();
		m.clean(2, null);
		for (int i=0;i<10;i++){
			
			List<Integer> iatoms=IntStream.range(0, m.getAtomCount()).mapToObj(i2->i2).collect(Collectors.toList());
			Collections.shuffle(iatoms);
			int[] map =iatoms.stream().mapToInt(i1->i1).toArray();
			Molecule s=shuffleMolecule(m,map);
			basicTest(s,spec.expectedLychi,spec.shouldMatch);
			m=s;
		}
	}
	
	
	@Parameterized.Parameters(name = "{0}")
	public static List<Object[]> data(){
		List<LychiTestInstance> tests = new ArrayList<>();
		
		tests.add(LychiTestInstance.of("CCCCCC","U28WVSD82-2YWKXKS36P-2PQPUKLGUWW-2PWK7BQNJSU6").name("simple carbon chain"));
		tests.add(LychiTestInstance.of("O=C(O[C@H]1C[C@H]2C[C@H]3C[C@@H](C1)N2CC3=O)C4=CNC5=C4C=CC=C5","38C4U16JU-UC5KDUPMVH-UHFJLJL661C-UHCRHDK74DXU").name("cage-like structure"));
		tests.add(LychiTestInstance.of("C[C@@H]1CC[C@@H](C)CC1","T75RBW5S8-8D9T563A7Y-8YC8NQXD9W5-8Y5MFVTVS3J3").name("trans across ring"));
		tests.add(LychiTestInstance.of("C[C@H]1CC[C@@H](C)CC1","T75RBW5S8-8D9T563A7Y-8YC8NQXD9W5-8Y5JH5RWXRLR").name("cis across ring"));
		tests.add(LychiTestInstance.of("[H][C@@]12[C@@H]3SC[C@]4(NCCC5=C4C=C(OC)C(O)=C5)C(=O)OC[C@H](N1[C@@H](O)[C@@H]6CC7=C([C@H]2N6C)C(O)=C(OC)C(C)=C7)C8=C9OCOC9=C(C)C(OC(C)=O)=C38", "DCLRH149F-FFMPLZ16VC-FC35942KGAU-FCUDSDS2V1NT").name("round trip problem"));
		
		
		tests.add(LychiTestInstance.equivalent("\n" + 
				"  Ketcher 12201304332D 1   1.00000     0.00000     0\n" + 
				"\n" + 
				" 59 67  0     1  0            999 V2000\n" + 
				"   -2.2321   -1.8660    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -1.7321   -1.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -2.5981   -0.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -2.5981    0.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -3.4641    1.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -3.4641    2.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -4.3301    2.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -2.5981    2.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -2.5981    3.5000    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -3.4641    4.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -1.7321    2.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -0.8660    2.5000    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -1.7321    1.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -0.8660    0.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -0.4740    1.2647    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -1.7321    0.0000    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -0.9071   -0.4750    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    0.0000    1.0000    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    0.0000   -1.0000    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -0.8660   -1.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -0.8660   -2.5000    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    0.8660   -1.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    0.8561   -2.3746    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    2.4488   -3.1947    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    4.5544   -3.0234    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    5.3132   -1.2000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    6.5741   -1.3179    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    4.8632    0.2250    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    4.0294    0.9234    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    2.9488    1.1197    0.0000 S   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    0.8660    0.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    0.8811    1.3246    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    1.7321    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    2.5981    0.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    2.4244    1.4848    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    1.6097    2.0768    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    0.7419    1.9858    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    1.7927    2.9165    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    3.4641    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    3.9301    0.3000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    3.4641   -1.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    4.2072   -1.6691    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    3.8005   -2.5827    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    2.8060   -2.4781    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    2.5981   -1.5000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    1.7321   -1.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    5.6506   -0.2222    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    6.4172    0.1894    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    6.4966    1.1232    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    5.8342    1.6954    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    6.0136    2.6792    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    5.2512    3.3264    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    5.4306    4.3102    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    4.3096    2.9897    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    3.5473    3.6370    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    3.7266    4.6207    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    4.1303    2.0060    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"    4.8676    1.3838    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  2  1  1  1     0  0\n" + 
				"  2  3  1  0     0  0\n" + 
				"  3  4  1  0     0  0\n" + 
				"  4  5  1  0     0  0\n" + 
				"  5  6  2  0     0  0\n" + 
				"  6  7  1  0     0  0\n" + 
				"  6  8  1  0     0  0\n" + 
				"  8  9  1  0     0  0\n" + 
				"  9 10  1  0     0  0\n" + 
				"  8 11  2  0     0  0\n" + 
				" 11 12  1  0     0  0\n" + 
				" 11 13  1  0     0  0\n" + 
				"  4 13  2  0     0  0\n" + 
				" 13 14  1  0     0  0\n" + 
				" 14 15  1  1     0  0\n" + 
				" 14 16  1  0     0  0\n" + 
				"  2 16  1  0     0  0\n" + 
				" 16 17  1  0     0  0\n" + 
				" 14 18  1  0     0  0\n" + 
				" 18 19  1  1     0  0\n" + 
				" 18 20  1  0     0  0\n" + 
				" 20 21  1  0     0  0\n" + 
				"  2 21  1  0     0  0\n" + 
				" 21 22  1  1     0  0\n" + 
				" 20 23  1  0     0  0\n" + 
				" 23 24  1  1     0  0\n" + 
				" 23 25  1  0     0  0\n" + 
				" 25 26  1  0     0  0\n" + 
				" 26 27  1  0     0  0\n" + 
				" 27 28  2  0     0  0\n" + 
				" 29 27  1  0     0  0\n" + 
				" 29 30  1  6     0  0\n" + 
				" 30 31  1  0     0  0\n" + 
				" 31 32  1  0     0  0\n" + 
				" 18 32  1  0     0  0\n" + 
				" 32 33  1  1     0  0\n" + 
				" 32 34  1  0     0  0\n" + 
				" 34 35  1  0     0  0\n" + 
				" 35 36  1  0     0  0\n" + 
				" 36 37  1  0     0  0\n" + 
				" 37 38  1  0     0  0\n" + 
				" 37 39  2  0     0  0\n" + 
				" 35 40  2  0     0  0\n" + 
				" 40 41  1  0     0  0\n" + 
				" 40 42  1  0     0  0\n" + 
				" 42 43  1  0     0  0\n" + 
				" 43 44  1  0     0  0\n" + 
				" 44 45  1  0     0  0\n" + 
				" 45 46  1  0     0  0\n" + 
				" 42 46  2  0     0  0\n" + 
				" 46 47  1  0     0  0\n" + 
				" 23 47  1  0     0  0\n" + 
				" 34 47  2  0     0  0\n" + 
				" 29 48  1  0     0  0\n" + 
				" 48 49  1  0     0  0\n" + 
				" 49 50  1  0     0  0\n" + 
				" 50 51  1  0     0  0\n" + 
				" 51 52  1  0     0  0\n" + 
				" 52 53  2  0     0  0\n" + 
				" 53 54  1  0     0  0\n" + 
				" 53 55  1  0     0  0\n" + 
				" 55 56  1  0     0  0\n" + 
				" 56 57  1  0     0  0\n" + 
				" 55 58  2  0     0  0\n" + 
				" 58 59  1  0     0  0\n" + 
				" 29 59  1  0     0  0\n" + 
				" 51 59  2  0     0  0\n" + 
				"M  END", "[H][C@@]12CC3=C(C(O)=C(OC)C(C)=C3)[C@@]([H])(N1C)[C@@]4([H])N([C@H]2O)[C@@]5([H])COC(=O)[C@]8(CS[C@]4([H])C6=C5C7=C(OCO7)C(C)=C6OC(C)=O)NCCC9=C8C=C(OC)C(O)=C9").name("strereo parity issue 1"));
		//C(C)1CCC(C)CC1
		tests.add(LychiTestInstance.equivalent("[C@H](C)1CCC(C)CC1","C(C)1CCC(C)CC1").name("meaningless streo on a ring shouldn't be honored"));
		tests.add(LychiTestInstance.equivalent("[C@H](C)1CCC(C)CC1","[C@@H](C)1CCC(C)CC1"));
		
		tests.add(LychiTestInstance.equivalent("C[C@H]1CC[C@@H](C)CC1","C[C@@H]1CC[C@H](C)CC1").name("opposite form of cis/trans on ring should be the same"));
		
		tests.add(LychiTestInstance.notEquivalent("C[C@H]1CC[C@@H](C)CC1","C[C@H]1CC[C@H](C)CC1").name("cis across ring is different from trans across ring"));
		
		
		tests.add(LychiTestInstance.equivalent("C[C@H]1C[C@@H](C)CC(C)C1","C[C@@H]1C[C@H](C)CC(C)C1").name("symmetric half-defined stereo should be the same"));
		
		//O[C@H]1CC(O)CC(O)C1
		tests.add(LychiTestInstance.equivalent("O[C@H]1CC(O)CC(O)C1","O[C@@H]1CC(O)CC(O)C1").name("3-center, 1 specified meaningless center should be same as inverted"));
		
		tests.add(LychiTestInstance.equivalent("C[C@H]1OC(C)O[C@@H](C)O1","CC1OC(C)OC(C)O1").name("meaningless stereo with 2 dashed bonds on ring shouldn't be honored"));
		
		//OC1C(O)C(O)C(O)[C@@H](O)[C@H]1O
		tests.add(LychiTestInstance.equivalent("OC1C(O)C(O)C(O)[C@@H](O)[C@H]1O","OC1C(O)C(O)C(O)[C@H](O)[C@@H]1O").name("semi-meaningful symmetric stereo honored"));
		tests.add(LychiTestInstance.notEquivalent("OC1C(O)C(O)C(O)[C@@H](O)[C@H]1O","OC1C(O)C(O)C(O)[C@@H](O)[C@@H]1O").name("distinct semi-meaningful symmetric stereo honored"));
		
		tests.add(LychiTestInstance.equivalent("OC1[C@H](O)[C@H](O)C1O","OC1[C@@H](O)[C@@H](O)C1O").name("4-center, 2 specified symmetric meaningful stereo should be same as inverted"));
		tests.add(LychiTestInstance.notEquivalent("OC1[C@H](O)[C@H](O)C1O","OC1[C@@H](O)[C@H](O)C1O").name("4-center, 2 specified symmetric meaningful stereo should not be same as 1 center modified"));
		
		
		//OC1[C@H](O)[C@H](O)C1O
		
		//C[C@H]1OC(C)O[C@@H](C)O1
		//[#6][C@H]1C[C@@H]([#6])CC([#6])C1.[#6][C@H]2CC([#6])C[C@@H]([#6])C2
		tests.add(LychiTestInstance.equivalent("\n" + 
				"  MJ150420                      \n" + 
				"\n" + 
				"  8  8  0  0  0  0  0  0  0  0999 V2000\n" + 
				"   -2.2656    0.8138    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -2.9801    0.4013    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -2.9801   -0.4237    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -2.2656   -0.8361    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -1.5511   -0.4237    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -1.5511    0.4013    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -0.8366    0.8138    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   -0.8366   -0.0111    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  1  0  0  0  0\n" + 
				"  1  6  1  0  0  0  0\n" + 
				"  2  3  1  0  0  0  0\n" + 
				"  3  4  1  0  0  0  0\n" + 
				"  4  5  1  0  0  0  0\n" + 
				"  5  6  1  0  0  0  0\n" + 
				"  6  7  1  1  0  0  0\n" + 
				"  6  8  1  6  0  0  0\n" + 
				"M  END","C1CCCCC1").name("meaningless stereo 1"));
		
		
		return tests.stream().map(ls->ls.asJunitInput()).collect(Collectors.toList());
	}
}