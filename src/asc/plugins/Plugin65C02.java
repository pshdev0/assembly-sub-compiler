package asc.plugins;

import asc.Plugin;
import asc.CompilerState;

public class Plugin65C02 extends Plugin {

    public Plugin65C02() {
        // source: https://wilsonminesco.com/NMOS-CMOSdif/

        /*

            BRAND NEW MNEMONICS

            instruction    op code
            & addr mode    (in hex)    description

            BRA rel          80       Branch Relative Always (unconditionally), range -128 to +127.

            PHX              DA       PusH X.  ‾⌉
            PLX              FA       PulL X.   |  No need to go through A for these anymore.
            PHY              5A       PusH Y.   |
            PLY              7A       PulL Y.  _⌋

            STZ  addr        9C       ‾⌉
            STZ  addr,X      9E        |  STore a Zero, regardless of what's in A, X, or Y.
            STZ  ZP          64        |  Processor registers are not affected by STZ.
            STZ  ZP,X        74       _⌋

            TRB  addr        1C       ‾⌉  Test & Reset memory Bits with A.
            TRB  ZP          14       _⌋

            TSB  addr        0C       ‾⌉  Test & Set memory Bits with A.
            TSB  ZP          04       _⌋

            BBR  ZP       0F-7F [1]   Branch if specified Bit is Reset. ‾⌉ These are most useful
            BBS  ZP       8F-FF [1]   Branch if specified Bit is Set.    | when I/O is in ZP.  They
            RMB  ZP       07-77 [1]   Reset specified Memory Bit.        | are on WDC & Rockwell but
            SMB  ZP       87-F7 [1]   Set specified Memory Bit.         _⌋ not GTE/CMD or Synertek.

            STP              DB       SToP the processor until the next RST.     ‾⌉
                                      Power-supply current drops to nearly zero.  |  These two are
                                                                                  |  on WDC only.
            WAI              CB       WAIt.  It's like STP, but any interrupt     |
                                      will make it resume execution.  Especially  |
                                      useful for superfast interrupt response,    |
                                      with zero latency.  See interrupts primer. _⌋

         */

        addMnemonic("bra rel 80");
        addMnemonic("phx imp da");
        addMnemonic("plx imp fa");
        addMnemonic("phy imp 5a");
        addMnemonic("ply imp 7a");
        addMnemonic("stz a 9c ax 9e zp 64 zpx 74");
        addMnemonic("trb a 1c zp 14");
        addMnemonic("tsb a 0c zp 04");

        addMnemonic("bbr0 zp 0f");
        addMnemonic("bbr1 zp 1f");
        addMnemonic("bbr2 zp 2f");
        addMnemonic("bbr3 zp 3f");
        addMnemonic("bbr4 zp 4f");
        addMnemonic("bbr5 zp 5f");
        addMnemonic("bbr6 zp 6f");
        addMnemonic("bbr7 zp 7f");

        addMnemonic("bbs0 zp 8f");
        addMnemonic("bbs1 zp 9f");
        addMnemonic("bbs2 zp af");
        addMnemonic("bbs3 zp bf");
        addMnemonic("bbs4 zp cf");
        addMnemonic("bbs5 zp df");
        addMnemonic("bbs6 zp ef");
        addMnemonic("bbs7 zp ff");

        addMnemonic("rmb0 zp 07");
        addMnemonic("rmb1 zp 17");
        addMnemonic("rmb2 zp 27");
        addMnemonic("rmb3 zp 37");
        addMnemonic("rmb4 zp 47");
        addMnemonic("rmb5 zp 57");
        addMnemonic("rmb6 zp 67");
        addMnemonic("rmb7 zp 77");

        addMnemonic("smb0 zp 87");
        addMnemonic("smb1 zp 97");
        addMnemonic("smb2 zp a7");
        addMnemonic("smb3 zp b7");
        addMnemonic("smb4 zp c7");
        addMnemonic("smb5 zp d7");
        addMnemonic("smb6 zp e7");
        addMnemonic("smb7 zp f7");

        addMnemonic("stp imp db"); // stop the processor until next rst (reduces power usage to ~ 0)
        addMnemonic("wai imp cb"); // same as stp but interrupts will resume

        /*

            BRAND NEW ADDRESSING MODES FOR EXISTING MNEMONICS

            instruction    op code
            & addr mode    (in hex)    description

            ADC  (addr)      72       ADC absolute indirect
            AND  (addr)      32       AND absolute indirect
            BIT  addr,X      3C       BIT absolute indexed
            BIT  ZP,X        34       BIT zero-page indexed
            BIT  #           89       BIT immediate
            CMP  (addr)      D2       CMP absolute indirect
            DEC  A           3A       DECrement accumulator (alternate mnemonic: DEA)
            INC  A           1A       INCrement accumulator (alternate mnemonic: INA)
            EOR  (addr)      52       EOR absolute indirect
            JMP  (addr,X)    7C       JMP absolute indexed indirect
            LDA  (addr)      B2       LDA absolute indirect
            ORA  (addr)      12       ORA absolute indirect
            SBC  (addr)      F2       SBC absolute indirect
            STA  (addr)      92       STA absolute indirect

         */

        addMnemonic("adc ind 72");
        addMnemonic("and ind 32");
        addMnemonic("bit ax 3c zpx 34");
        addMnemonic("cmp ind d2");
        addMnemonic("dec imp 3a");
        addMnemonic("inc imp 1a");
        addMnemonic("eor ind 52");
        addMnemonic("jmp ix 7c");
        addMnemonic("lda ind b2");
        addMnemonic("ora ind 12");
        addMnemonic("sbc ind f2");
        addMnemonic("sta ind 92");
    }

    @Override
    public boolean commandBlocks(CompilerState state) throws Exception {
        return false;
    }

    @Override
    public boolean functionNoParams(CompilerState state) throws Exception {
        return false;
    }

    @Override
    public boolean functionWithParams(CompilerState state) throws Exception {
        return false;
    }
}
