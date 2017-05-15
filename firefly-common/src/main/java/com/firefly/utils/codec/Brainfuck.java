package com.firefly.utils.codec;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class Brainfuck {

    public enum OpT {INC, MOVE, PRINT, LOOP}

    public static class Op {
        public OpT op;
        public int v;
        public List<Op> loop;

        public Op(OpT _op, int _v) {
            op = _op;
            v = _v;
            loop = new ArrayList<>();
        }

        public Op(OpT _op, List<Op> _l) {
            op = _op;
            loop = _l;
            v = 0;
        }
    }

    public static class CharacterIterator {
        private final String str;
        private int pos = 0;

        public CharacterIterator(String str) {
            this.str = str;
        }

        public boolean hasNext() {
            return pos < str.length();
        }

        public Character next() {
            return str.charAt(pos++);
        }
    }

    public static class Tape {
        private int[] tape;
        private int pos;

        public Tape() {
            tape = new int[1];
        }

        public int get() {
            return tape[pos];
        }

        public void inc(int x) {
            tape[pos] += x;
        }

        public void move(int x) {
            pos += x;
            while (pos >= tape.length) {
                int[] tape = new int[this.tape.length * 2];
                System.arraycopy(this.tape, 0, tape, 0, this.tape.length);
                this.tape = tape;
            }

        }
    }

    public static class Program {
        private List<Op> ops;

        public Program(String code) {
            CharacterIterator it = new CharacterIterator(code);
            ops = parse(it);
        }

        private List<Op> parse(CharacterIterator it) {
            List<Op> res = new ArrayList<>();
            while (it.hasNext()) {
                switch (it.next()) {
                    case '+':
                        res.add(new Op(OpT.INC, 1));
                        break;
                    case '-':
                        res.add(new Op(OpT.INC, -1));
                        break;
                    case '>':
                        res.add(new Op(OpT.MOVE, 1));
                        break;
                    case '<':
                        res.add(new Op(OpT.MOVE, -1));
                        break;
                    case '.':
                        res.add(new Op(OpT.PRINT, 0));
                        break;
                    case '[':
                        res.add(new Op(OpT.LOOP, parse(it)));
                        break;
                    case ']':
                        return res;
                }
            }
            return res;
        }

        public void run() {
            _run(ops, new Tape());
        }

        private void _run(List<Op> program, Tape tape) {
            for (Op op : program) {
                switch (op.op) {
                    case INC:
                        tape.inc(op.v);
                        break;
                    case MOVE:
                        tape.move(op.v);
                        break;
                    case LOOP:
                        while (tape.get() > 0) _run(op.loop, tape);
                        break;
                    case PRINT:
                        System.out.print((char) tape.get());
                        break;
                }
            }
        }
    }

    public static void main(String[] args) {
        String code = "++++++++[>+>++>+++>++++>+++++>++++++>+++++++>++++++++>+++++++++>++++++++++>+++++++++++>++++++++++++>+++++++++++++>++++++++++++++>+++++++++++++++>++++++++++++++++<<<<<<<<<<<<<<<<-]>>>>>>>>>>>----.++++<<<<<<<<<<<>>>>>>>>>>>>>.<<<<<<<<<<<<<>>>>>>>>>>>>>---.+++<<<<<<<<<<<<<>>>>.<<<<>>>>>>>>>>>>>>.<<<<<<<<<<<<<<>>>>>>>>>>>>>>++.--<<<<<<<<<<<<<<>>>>>>>>>>>>>>-.+<<<<<<<<<<<<<<>>>>>>>>>>>>>++.--<<<<<<<<<<<<<>>>>>>>>>>>>>---.+++<<<<<<<<<<<<<>>>>>>>>>>>>+++.---<<<<<<<<<<<<>>>>>>>>>>>>>>>----.++++<<<<<<<<<<<<<<<>>>>.<<<<>>>>>>>>>>>>>>>---.+++<<<<<<<<<<<<<<<>>>>>>>>>>>>>>++.--<<<<<<<<<<<<<<>>>>>>>>>>>>>>----.++++<<<<<<<<<<<<<<>>>>>>>++.--<<<<<<<>>>>.<<<<>>>>>>>>>>>>>.<<<<<<<<<<<<<>>>>>>>>>>>>>>>----.++++<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>----.++++<<<<<<<<<<<<<<<>>>>>>>>>>>>>>.<<<<<<<<<<<<<<>>>>>>>>>>>>>>+++.---<<<<<<<<<<<<<<>>>>>>>++.--<<<<<<<>>>>>>-.+<<<<<<>>>>>>-.+<<<<<<>>>>>>>>>>>>>-.+<<<<<<<<<<<<<>>>>>>>>>>>>>+.-<<<<<<<<<<<<<>>>>>>>>>>>>>>>----.++++<<<<<<<<<<<<<<<>>>>>>>>>>>>>.<<<<<<<<<<<<<>>>>>>>>>>>>>>>---.+++<<<<<<<<<<<<<<<>>>>>>>>>>>>++.--<<<<<<<<<<<<>>>>>>--.++<<<<<<>>>>>>>>>>>>+++.---<<<<<<<<<<<<>>>>>>>>>>>>>>-.+<<<<<<<<<<<<<<>>>>>>>>>>>>>>---.+++<<<<<<<<<<<<<<>>>>>>-.+<<<<<<>>>>>>>>>>>>>.<<<<<<<<<<<<<>>>>>>>>>>>>>>>+.-<<<<<<<<<<<<<<<>>>>>>>>>>>>>>.<<<<<<<<<<<<<<>>>>>>>>>>>>>---.+++<<<<<<<<<<<<<>>>>>>>>>>>>>>++.--<<<<<<<<<<<<<<>>>>>>>>>>>>+++.---<<<<<<<<<<<<>>>>>>>>>>>>>>>---.+++<<<<<<<<<<<<<<<>>>>>>>>>>>>++.--<<<<<<<<<<<<>>>>>>>>>>>>>---.+++<<<<<<<<<<<<<>>>>>>+.-<<<<<<>>>>>>.<<<<<<>>>>>>++.--<<<<<<>>>>>>>----.++++<<<<<<<>>>>>>-.+<<<<<<>>>>>>>>>>>>>>---.+++<<<<<<<<<<<<<<>>>>>>>>>>>>>>-.+<<<<<<<<<<<<<<>>>>>>>>>>>>>>-.+<<<<<<<<<<<<<<>>>>>>>>>>>>>>--.++<<<<<<<<<<<<<<>>>>>>>>>>>>>>----.++++<<<<<<<<<<<<<<>>>>>>>>>>>>>+.-<<<<<<<<<<<<<>>>>>>>>>>>>>-.+<<<<<<<<<<<<<>>>>>>>>>>>>>.<<<<<<<<<<<<<>>>>>>>>>>>>>>>----.++++<<<<<<<<<<<<<<<.";
        Program program = new Program(code);
        program.run();
    }
}
