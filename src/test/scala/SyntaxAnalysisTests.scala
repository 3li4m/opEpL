/*
 * This file is part of COMP332 Assignment 2/3 2019.
 *
 * weBCPL, a retro BCPL to WebAssembly compiler.
 *
 * © 2019, Dominic Verity and Anthony Sloane, Macquarie University.
 *         All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Tests of the parser of the BCPL language.
 */

package webcpl

import org.bitbucket.inkytonik.kiama.util.{ParseTests}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
  * Tests that check that the parser works correctly.  I.e., it accepts correct
  * input and produces the appropriate trees, and it rejects illegal input.
  */
@RunWith(classOf[JUnitRunner])
class SyntaxAnalysisTests extends ParseTests with ParseTestExtras {

  import BCPLTree._

  val parsers = new SyntaxAnalysis(positions)

  import parsers._

  /*
   * Tests of lexeme parsing of constants and identifiers.
   */

  test("parsing an identifier of one letter produces the correct tree") {
    identifier("x") should parseTo[String]("x")
  }

  test("parsing an identifier as an identifier produces the correct tree") {
    identifier("count") should parseTo[String]("count")
  }

  test("parsing an identifier containing digits and underscores") {
    identifier("x1_2_3") should parseTo[String]("x1_2_3")
  }

  test("parsing an integer as an identifier gives an error") {
    identifier("42") should failParseAt(1, 1, "expecting identifier")
  }

  test("parsing a non-identifier as an identifier gives an error (digit)") {
    identifier("4foo") should
      failParseAt(1, 1, "expecting identifier")
  }

  test("parsing a non-identifier as an identifier gives an error (underscore)") {
    identifier("_f3") should
      failParseAt(1, 1, "expecting identifier")
  }

  test("parsing a keyword as an identifier gives an error") {
    identifier("LET ") should
      failParseAt(1, 1, "expecting identifier but keyword found")
  }

  test("parsing a keyword prefix as an identifier produces the correct tree") {
    identifier("LETTER") should parseTo[String]("LETTER")
  }

  test(
    "parsing a decimal constant of one digit produces the correct integer"
  ) {
    integerConst("8") should parseTo[Int](8)
  }

  test("parsing a decimal constant produces the correct integer") {
    integerConst("99") should parseTo[Int](99)
  }

  test("parsing a decimal constant with underscores") {
    integerConst("123_456_789") should parseTo[Int](123456789)
  }

  test("parsing a hex constant produces the correct integer") {
    integerConst("#xFED") should parseTo[Int](4077)
  }

  test("parsing #xFFFFFFFF produces the integer -1") {
    integerConst("#xFFFFFFFF") should parseTo[Int](-1)
  }

  test("parsing #x80000000 produces the integer -2147483648") {
    integerConst("#x80000000") should parseTo[Int](-2147483648)
  }

  test("parsing #x_DeadC0de produces the integer -559038242") {
    integerConst("#x_DeadC0de") should parseTo[Int](-559038242)
  }

  test("parsing a non-integer as an integer gives an error") {
    integerConst("total") should
      failParseAt(1, 1, "expecting integer literal")
  }

  test("parsing string constant with BCPL style escapes") {
    stringConst("""  "This *n is *b a *t test *x7F"  """) should
      parseTo[Vector[Int]](
        Vector(
          84, 104, 105, 115, 32, 10, 32, 105, 115, 32, 8, 32, 97,
          32, 9, 32, 116, 101, 115, 116, 32, 127))
  }

  test("parsing incorrect unicode escape fails") {
    charConst("'*u01F63X'") should failParseAt(1, 4, "illegal unicode codepoint")
  }

  test("parsing unicode escape with invalid codepoint fails") {
    charConst("'*u41F63A'") should failParseAt(1, 4, "illegal unicode codepoint")
  }

  test("parsing string constant with unicode escape") {
    stringConst(""""*u01F63A😺"""") should parseTo[Vector[Int]](
      Vector(240, 159, 152, 186, 240, 159, 152, 186))
  }

  test("parsing unicode character constant from higher plane") {
    charConst("'😺'") should parseTo(codepointToUTF8("😺".codePointAt(0)))
  }

  test("parsing incorrect hex ASCII escape fails") {
    charConst("'*xA7'") should failParseAt(1, 4, "illegal hex ASCII code")
  }

  test("parsing hex ASCII escapes in a string") {
    stringConst(""""*x48ell*x6f *x57or*x6cd*x21"""") should parseTo[Vector[Int]](
      Vector(72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33))
  }

  test("parsing incorrect octal ASCII escape fails") {
    charConst("'*407'") should failParseAt(1, 3, "illegal octal ASCII code")
  }

  test("parsing of string constants ignores * fenced whitespace") {
    stringConst(
      """   "This is a test, *  // Comment
            *here is some more"""") should parseTo[Vector[Int]](
      Vector(
        84, 104, 105, 115, 32, 105, 115, 32, 97, 32, 116, 101,
        115, 116, 44, 32, 104, 101, 114, 101, 32, 105, 115, 32,
        115, 111, 109, 101, 32, 109, 111, 114, 101))
  }

  test("parsing line end character in string literal fails") {
    stringConst(
      """|"Hello
         |World!"""".stripMargin) should
      failParseAt(1, 7, "unterminated string literal")
  }

  test("parsing of empty character literal fails") {
    charConst("''") should
      failParseAt(1, 2, "empty character literal")
  }

  test("parsing of * fenced whitespace in character literal fails") {
    charConst("'*  *'") should
      failParseAt(1, 2, "empty character literal")
  }

  test("parsing unterminated character literal fails (1)") {
    charConst("""'*015"""") should
      failParseAt(1, 6, "unterminated character literal")
  }

  test("parsing character literal encompassing end of line fails") {
    charConst(
      """|'
         |''""".stripMargin) should
      failParseAt(1, 2, "end of line/input in character literal")
  }

  test("parsing character literal interrupted by end of input fails") {
    charConst("'") should
      failParseAt(1, 2, "end of line/input in character literal")
  }
  test("parsing 'TRUE'") {
    phrase(expression)(" TRUE ") should parseTo[Expression](TrueExp())
  }

  test("parsing 'FALSE'") {
    phrase(expression)(" FALSE ") should parseTo[Expression](FalseExp())
  }

  /*
   * Tests of declaration parsing.
   */

  // `GLOBAL` declarations.

  test("parsing 'GLOBAL' declaration with no entries should fail") {
    phrase(declaration)("GLOBAL { }") should failParseAt(1, 10)
  }

  test("parsing 'GLOBAL' declaration with a single entry") {
    phrase(declaration)("GLOBAL { getch : 1 }") should
      parseTo[Declaration](
        GlobalDecl(Vector(GlobalEntry(IdnDef("getch"), Some(IntExp(1))))))
  }

  test("parsing simple 'GLOBAL' declaration") {
    phrase(declaration)("GLOBAL { getch : 1; putch : 2 }") should
      parseTo[Declaration](
        GlobalDecl(
          Vector(
            GlobalEntry(IdnDef("getch"), Some(IntExp(1))),
            GlobalEntry(IdnDef("putch"), Some(IntExp(2)))))
      )
  }

  test("parsing 'GLOBAL' declaration with EOL separator") {
    phrase(declaration)(
      """|
         |GLOBAL {
         |  getch : 1
         |  putch : 2
         |}""".stripMargin) should
      parseTo[Declaration](
        GlobalDecl(
          Vector(
            GlobalEntry(IdnDef("getch"), Some(IntExp(1))),
            GlobalEntry(IdnDef("putch"), Some(IntExp(2)))))
      )
  }

  test("parsing 'GLOBAL' declaration with missing initialisers") {
    phrase(declaration)("GLOBAL $( init : 3; getch; putch $)") should
      parseTo[Declaration](
        GlobalDecl(
          Vector(
            GlobalEntry(IdnDef("init"), Some(IntExp(3))),
            GlobalEntry(IdnDef("getch"), None),
            GlobalEntry(IdnDef("putch"), None)))
      )
  }

  // `STATIC` declarations.

  test("parsing 'STATIC' declaration with no entries should fail") {
    phrase(declaration)("STATIC { }") should failParseAt(1, 10)
  }

  test("parsing 'STATIC' declaration with a single entry") {
    phrase(declaration)("STATIC { charCount = 256 }") should
      parseTo[Declaration](
        StaticDecl(
          Vector(StaticEntry(IdnDef("charCount"), Some(IntExp(256))))
        ))
  }

  test("parsing simple 'STATIC' declaration") {
    phrase(declaration)("STATIC { charCount = 256; endOfLine = FALSE }") should
      parseTo[Declaration](
        StaticDecl(
          Vector(
            StaticEntry(IdnDef("charCount"), Some(IntExp(256))),
            StaticEntry(IdnDef("endOfLine"), Some(FalseExp()))
          )))
  }

  test("parsing 'STATIC' declaration with EOL separator") {
    phrase(declaration)(
      """|STATIC { 
         |  charCount = 256
         |  endOfLine = FALSE
         |  char = 'A'
         |}""".stripMargin) should
      parseTo[Declaration](
        StaticDecl(
          Vector(
            StaticEntry(IdnDef("charCount"), Some(IntExp(256))),
            StaticEntry(IdnDef("endOfLine"), Some(FalseExp())),
            StaticEntry(IdnDef("char"), Some(ChrExp(Vector(65))))
          )))
  }

  test("parsing 'STATIC' declaration with missing initialisers") {
    phrase(declaration)("STATIC $( x1; x2 = 10; x3 $)") should
      parseTo[Declaration](
        StaticDecl(
          Vector(
            StaticEntry(IdnDef("x1"), None),
            StaticEntry(IdnDef("x2"), Some(IntExp(10))),
            StaticEntry(IdnDef("x3"), None)))
      )
  }

  // `MANIFEST` declarations.

  test("parsing 'MANIFEST' declaration with no entries should fail") {
    phrase(declaration)("MANIFEST { }") should failParseAt(1, 12)
  }

  test("parsing 'MANIFEST' declaration with a single entry") {
    phrase(declaration)("MANIFEST { charCount = 256 }") should
      parseTo[Declaration](
        ManifestDecl(
          Vector(ManifestEntry(IdnDef("charCount"), Some(IntExp(256))))
        ))
  }

  test("parsing simple 'MANIFEST' declaration") {
    phrase(declaration)("MANIFEST { charCount = 256; endOfLine = FALSE }") should
      parseTo[Declaration](
        ManifestDecl(
          Vector(
            ManifestEntry(IdnDef("charCount"), Some(IntExp(256))),
            ManifestEntry(IdnDef("endOfLine"), Some(FalseExp()))
          )))
  }

  test("parsing 'MANIFEST' declaration with EOL separator") {
    phrase(declaration)(
      """|MANIFEST { 
         |  charCount = 256
         |  endOfLine = FALSE
         |  char = 'A'
         |}""".stripMargin) should
      parseTo[Declaration](
        ManifestDecl(
          Vector(
            ManifestEntry(IdnDef("charCount"), Some(IntExp(256))),
            ManifestEntry(IdnDef("endOfLine"), Some(FalseExp())),
            ManifestEntry(IdnDef("char"), Some(ChrExp(Vector(65))))
          )))
  }

  test("parsing 'MANIFEST' declaration with missing initialisers") {
    phrase(declaration)("MANIFEST $( x1; x2 = 10; x3 $)") should
      parseTo[Declaration](
        ManifestDecl(
          Vector(
            ManifestEntry(IdnDef("x1"), None),
            ManifestEntry(IdnDef("x2"), Some(IntExp(10))),
            ManifestEntry(IdnDef("x3"), None)))
      )
  }

  // `LET` declarations of variables

  test("parse of variable declaration without initialiser should fail") {
    phrase(declaration)("LET x =") should failParseAt(1, 8)
  }

  test("parse of variable declaration without equals should fail") {
    phrase(declaration)("LET x 28") should failParseAt(1, 7)
  }

  test("parse declaration of a single variable") {
    phrase(declaration)("LET x = 28") should parseTo[Declaration](
      LetDecl(
        Vector(
          LetVarClause(
            Vector(IdnDef("x")),
            Vector(IntExp(28))))))
  }

  test("parse declaration of two variables") {
    phrase(declaration)("LET x, y = 28, ?") should parseTo[Declaration](
      LetDecl(
        Vector(
          LetVarClause(
            Vector(IdnDef("x"), IdnDef("y")),
            Vector(IntExp(28), UndefExp())))))
  }

  test("parse declaration of four variables") {
    phrase(declaration)("LET x, y, width, height = 28, ?, SIZE, SIZE") should
      parseTo[Declaration](
        LetDecl(
          Vector(
            LetVarClause(
              Vector(
                IdnDef("x"), IdnDef("y"),
                IdnDef("width"), IdnDef("height")),
              Vector(
                IntExp(28), UndefExp(),
                IdnExp(IdnUse("SIZE")), IdnExp(IdnUse("SIZE")))))))
  }

  // `LET` declaration of `VEC`tors.

  test("parse of vector declaration missing equals fails") {
    phrase(declaration)("LET v VEC 10") should failParseAt(1, 7)
  }

  test("parse of vector declaration missing size expression fails") {
    phrase(declaration)("LET v = VEC") should failParseAt(1, 12)
  }

  test("parse simple vector declaration") {
    phrase(declaration)("LET v = VEC 10") should parseTo[Declaration](
      LetDecl(
        Vector(
          LetVecClause(IdnDef("v"), IntExp(10)))))
  }

  test("parse vector declaration with size given by manifest constant") {
    phrase(declaration)("LET c = VEC SIZE") should parseTo[Declaration](
      LetDecl(
        Vector(
          LetVecClause(IdnDef("c"), IdnExp(IdnUse("SIZE"))))))
  }

  // `LET` declarations of functions

  test("parsing a function declaration without equals fails") {
    phrase(declaration)("LET f(x) x") should failParseAt(1, 10)
  }

  test("parsing a function declaration without defining expression fails") {
    phrase(declaration)("LET f() =") should failParseAt(1, 10)
  }

  test("parse a function declaration with no formal parameters") {
    phrase(declaration)("LET f() = 10") should parseTo[Declaration](
      LetDecl(
        Vector(
          LetFnClause(IdnDef("f"), Vector(), IntExp(10)))))
  }

  test("parse a function declaration with a single formal parameter") {
    phrase(declaration)("LET hello(x) = x") should parseTo[Declaration](
      LetDecl(
        Vector(
          LetFnClause(IdnDef("hello"), Vector(IdnDef("x")), IdnExp(IdnUse("x"))))))
  }

  test("parse a function declaration with multiple parameters") {
    phrase(declaration)("LET g(u, v, w, x, y, z) = f(u, z)") should
      parseTo[Declaration](
        LetDecl(
          Vector(
            LetFnClause(
              IdnDef("g"),
              Vector(
                IdnDef("u"), IdnDef("v"), IdnDef("w"),
                IdnDef("x"), IdnDef("y"), IdnDef("z")),
              CallExp(
                IdnExp(IdnUse("f")),
                Vector(IdnExp(IdnUse("u")), IdnExp(IdnUse("z"))))))))
  }

  // `LET` declarations of procedures

  test("parsing a procedure declaration without `BE` fails") {
    phrase(declaration)("LET f(x) FINISH") should failParseAt(1, 10)
  }

  test("parsing a procudere declaration without defining statement fails") {
    phrase(declaration)("LET f() BE") should failParseAt(1, 11)
  }

  test("parse a procedure declaration with no formal parameters") {
    phrase(declaration)("LET f() BE FINISH") should parseTo[Declaration](
      LetDecl(
        Vector(
          LetProcClause(IdnDef("f"), Vector(), FinishStmt()))))
  }

  test("parse a procedure declaration with a single formal parameter") {
    phrase(declaration)("LET hello(x) BE FINISH") should parseTo[Declaration](
      LetDecl(
        Vector(
          LetProcClause(IdnDef("hello"), Vector(IdnDef("x")), FinishStmt()))))
  }

  test("parse a procedure declaration with multiple parameters") {
    phrase(declaration)("LET g(u, v, w, x, y, z) BE FINISH") should
      parseTo[Declaration](
        LetDecl(
          Vector(
            LetProcClause(
              IdnDef("g"),
              Vector(
                IdnDef("u"), IdnDef("v"), IdnDef("w"),
                IdnDef("x"), IdnDef("y"), IdnDef("z")),
              FinishStmt()))))
  }

  // `LET` declaration with `AND` clauses.

  test("parse `LET` declaration with single `AND` clause") {
    phrase(declaration)(
      """|LET a = VEC SIZE
         |AND f(x) = x""".stripMargin) should parseTo[Declaration](
      LetDecl(
        Vector(
          LetVecClause(IdnDef("a"), IdnExp(IdnUse("SIZE"))),
          LetFnClause(
            IdnDef("f"), Vector(IdnDef("x")), IdnExp(IdnUse("x"))))))
  }

  test("parse a more complex `LET` declaration with `AND` clauses") {
    phrase(declaration)(
      """|LET a, b = 10, ?
         |AND c = VEC 22
         |AND f(x) = x
         |AND g() BE FINISH""".stripMargin) should parseTo[Declaration](
      LetDecl(
        Vector(
          LetVarClause(
            Vector(IdnDef("a"), IdnDef("b")),
            Vector(IntExp(10), UndefExp())),
          LetVecClause(IdnDef("c"), IntExp(22)),
          LetFnClause(IdnDef("f"), Vector(IdnDef("x")), IdnExp(IdnUse("x"))),
          LetProcClause(IdnDef("g"), Vector(), FinishStmt()))))
  }

  /*
   * Tests of expression parsing.
   */

  // Function call expressions.

  test("parse simple function call with no argument") {
    phrase(expression)("getch()") should parseTo[Expression](
      CallExp(IdnExp(IdnUse("getch")), Vector()))
  }

  test("parse simple function call with one argument") {
    phrase(expression)("putch(#x0D)") should parseTo[Expression](
      CallExp(IdnExp(IdnUse("putch")), Vector(IntExp(13)))
    )
  }

  test("parse simple function call with more than one argument") {
    phrase(expression)("""writef("Count: %n, Success: %b", count, TRUE)""") should
      parseTo[Expression](
        CallExp(
          IdnExp(IdnUse("writef")),
          Vector(
            StringExp(
              Vector(
                67, 111, 117, 110, 116, 58, 32, 37, 110, 44, 32, 83,
                117, 99, 99, 101, 115, 115, 58, 32, 37, 98)),
            IdnExp(IdnUse("count")),
            TrueExp())))
  }

  test("parsing function call with missing argument causes an error") {
    phrase(expression)("""writef("Test: %n", )""") should failParseAt(1, 20)
  }

  test("parse of an iterated function call associates to the left") {
    phrase(expression)("f(1,2,c)(d,e)()") should parseTo[Expression](
      CallExp(
        CallExp(
          CallExp(
            IdnExp(IdnUse("f")),
            Vector(IntExp(1), IntExp(2), IdnExp(IdnUse("c")))),
          Vector(IdnExp(IdnUse("d")), IdnExp(IdnUse("e")))),
        Vector()))
  }

  // Relational operator (`=`, `~=`, `<`, `>`, `<=`, `>=`) expressions.

  test("parsing a single relation") {
    phrase(expression)("a <= b") should parseTo[Expression](
      LessOrEqualExp(IdnExp(IdnUse("a")), IdnExp(IdnUse("b")))
    )
  }

  test("parsing a sequence of two relations") {
    phrase(expression)("a = b = c") should parseTo[Expression](
      AndExp(
        EqualExp(IdnExp(IdnUse("a")), IdnExp(IdnUse("b"))),
        EqualExp(IdnExp(IdnUse("b")), IdnExp(IdnUse("c")))))
  }

  test("parsing a sequence of three relations") {
    phrase(expression)("a < b = c >= d") should parseTo[Expression](
      AndExp(
        AndExp(
          LessExp(IdnExp(IdnUse("a")), IdnExp(IdnUse("b"))),
          EqualExp(IdnExp(IdnUse("b")), IdnExp(IdnUse("c")))),
        GreaterOrEqualExp(IdnExp(IdnUse("c")), IdnExp(IdnUse("d")))))
  }

  test("parsing a sequence of four relations") {
    phrase(expression)("a > b = c <= d ~= e") should parseTo[Expression](
      AndExp(
        AndExp(
          AndExp(
            GreaterExp(IdnExp(IdnUse("a")), IdnExp(IdnUse("b"))),
            EqualExp(IdnExp(IdnUse("b")), IdnExp(IdnUse("c")))),
          LessOrEqualExp(IdnExp(IdnUse("c")), IdnExp(IdnUse("d")))),
        NotEqualExp(IdnExp(IdnUse("d")), IdnExp(IdnUse("e")))))
  }

  // FIXME Add your automated tests here.
  test("Test condExp") {
    phrase(expression)("a -> b, c") should parseTo[Expression](
      IfExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test condExp Associativity") {
    phrase(expression)("a -> b, c -> d, e") should parseTo[Expression](
      IfExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")),
        IfExp(
          IdnExp(IdnUse("c")),
          IdnExp(IdnUse("d")),
          IdnExp(IdnUse("e"))
        )))
  }
  test("Test EQV") {
    phrase(expression)("a EQV b") should parseTo[Expression](
      EqvExp(IdnExp(
        IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test EQV Associativity") {
    phrase(expression)("a EQV b EQV c") should parseTo[Expression](
      EqvExp(
        EqvExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
        ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test XOR") {
    phrase(expression)("a XOR b") should parseTo[Expression](
      XorExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test XOR Associativity") {
    phrase(expression)("a XOR b XOR c") should parseTo[Expression](
      XorExp(
        XorExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
        ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test OR |") {
    phrase(expression)("a | b") should parseTo[Expression](
      OrExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test OR | Associativity") {
    phrase(expression)("a | b | c") should parseTo[Expression](
      OrExp(
        OrExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
        ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test And &") {
    phrase(expression)("a & b") should parseTo[Expression](
      AndExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test And & Associativity") {
    phrase(expression)("a & b & c") should parseTo[Expression](
      AndExp(
        AndExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
        ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test Not") {
    phrase(expression)("NOT b") should parseTo[Expression](
      NotExp(IdnExp(IdnUse("b")))
    )
  }
  test("Test ShiftLeft <<") {
    phrase(expression)("a << b") should parseTo[Expression](
      ShiftLeftExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test ShiftLeft << Associativity") {
    phrase(expression)("a << b << c") should parseTo[Expression](
      ShiftLeftExp(
        ShiftLeftExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
        ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test ShiftRight >>") {
    phrase(expression)("a >> b") should parseTo[Expression](
      ShiftRightExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test ShiftRight >> Associativity") {
    phrase(expression)("a >> b >> c") should parseTo[Expression](
      ShiftRightExp(
        ShiftRightExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
        ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test ShiftRight >> ShiftLeft <<") {
    phrase(expression)("a >> b << c") should parseTo[Expression](
      ShiftLeftExp(
        ShiftRightExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
        ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test Plus a + b") {
    phrase(expression)("a + b") should parseTo[Expression](
      PlusExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test Plus a + b + c") {
    phrase(expression)("a + b + c") should parseTo[Expression](
      PlusExp(
        PlusExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
      ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test Minus a - b") {
    phrase(expression)("a - b") should parseTo[Expression](
      MinusExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test Minus a - b - c") {
    phrase(expression)("a - b - c") should parseTo[Expression](
      MinusExp(
        MinusExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
      ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test Unary Negate -") {
    phrase(expression)("-a") should parseTo[Expression](
      NegExp(IdnExp(IdnUse("a")))
    )
  }
  test("Test Unary Plus +") {
    phrase(expression)("+a") should parseTo[Expression](
      IdnExp(IdnUse("a"))
    )
  }
  test("Test Unary ABS") {
    phrase(expression)("ABS a") should parseTo[Expression](
      AbsExp(IdnExp(IdnUse("a")))
    )
  }
  test("Test Multiplication a * b") {
    phrase(expression)("a * b") should parseTo[Expression](
      StarExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test Multiplication a * b * c") {
    phrase(expression)("a * b * c") should parseTo[Expression](
      StarExp(
        StarExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
        ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test Divide a / b") {
    phrase(expression)("a / b") should parseTo[Expression](
      SlashExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test Divide a / b / c") {
    phrase(expression)("a / b / c") should parseTo[Expression](
      SlashExp(
        SlashExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
        ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test Mod a MOD b") {
    phrase(expression)("a MOD b") should parseTo[Expression](
      ModExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test Mod a MOD b MOD c") {
    phrase(expression)("a MOD b MOD c") should parseTo[Expression](
      ModExp(
        ModExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
        ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test UnaryPling !") {
    phrase(expression)("!abc") should parseTo[Expression](
      UnaryPlingExp(IdnExp(IdnUse("abc")))
    )
  }
  test("Test UnaryPercent %") {
    phrase(expression)("%abc") should parseTo[Expression](
      UnaryBytePlingExp(IdnExp(IdnUse("abc")))
    )
  }
  test("Test at @") {
    phrase(expression)("@abc") should parseTo[Expression](
      AddrOfExp (IdnExp(IdnUse("abc")))
    )
  }
  test("Test BinaryPling a ! b") {
    phrase(expression)("a ! b") should parseTo[Expression](
      BinaryPlingExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test BinaryPling a ! b ! c") {
    phrase(expression)("a ! b ! c") should parseTo[Expression](
      BinaryPlingExp(
        BinaryPlingExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
        ),
        IdnExp(IdnUse("c")))
    )
  }
  test("Test BinaryBytePling a % b") {
    phrase(expression)("a % b") should parseTo[Expression](
      BinaryBytePlingExp(
        IdnExp(IdnUse("a")),
        IdnExp(IdnUse("b")))
    )
  }
  test("Test BinaryBytePling a % b % c") {
    phrase(expression)("a % b % c") should parseTo[Expression](
      BinaryBytePlingExp(
        BinaryBytePlingExp(
          IdnExp(IdnUse("a")),
          IdnExp(IdnUse("b"))
        ),
        IdnExp(IdnUse("c")))
    )
  }
  test("test assign") {
    phrase(statement)("a := b") should parseTo[Statement](
      AssignStmt(
        Vector(IdnExp(IdnUse("a"))),
        Vector(IdnExp(IdnUse("b")))
      )
    )
  }

  test("parse declaration of a var") {
    phrase(statement)("? := res2") should parseTo[Statement](
      AssignStmt(
        Vector(UndefExp()),
        Vector(IdnExp(IdnUse("res2")))
      )
    )
  }
  test("oio") {
    phrase(declaration)("LET res1, res2, temp = 0, 1, ?") should
      parseTo[Declaration](
        LetDecl(
          Vector(
            LetVarClause(
              Vector(
                IdnDef("res1"), IdnDef("res2"),
                IdnDef("temp")),
              Vector(
                IntExp(0),IntExp(1),
                UndefExp())))))
  }
  test("assign a to res2") {
    phrase(statement)("a := res2") should parseTo[Statement](
      AssignStmt(
        Vector(IdnExp(IdnUse("a"))),
        Vector(IdnExp(IdnUse("res2")))
      )
    )
  }
  test("assign a ") {
    phrase(program)(
      """
      |GLOBAL {
      |   start:  1
      |   writef: 94
      |}

      |LET Fibonacci(n) = VALOF
      |{
      |    LET res1, res2, temp = 0, 1, ?

      |    WHILE 0 < n DO {
      |       temp := res1
      |       res1 := res2
      |       res2 := temp + res1
      |        n := n - 1
      |   }

      | RESULTIS res1
      |}

      |LET start() BE
      |{
      |   writef(
      |       "The 10th Fibonacci number is %n*n",
      |        fibonacci(n))
      |}""".stripMargin) should parseTo[Program](
      Program(
        Vector(
          GlobalDecl(
            Vector(
              GlobalEntry(IdnDef("start"), Some(IntExp(1))),
              GlobalEntry(IdnDef("writef"), Some(IntExp(94))))),
          LetDecl(
            Vector(
              LetFnClause(
                IdnDef("Fibonacci"),
                Vector(IdnDef("n")),
                ValofExp(
                  Block(
                    Vector(
                      LetDecl(
                        Vector(
                          LetVarClause(
                            Vector(IdnDef("res1"), IdnDef("res2"), IdnDef("temp")),
                            Vector(IntExp(0), IntExp(1), UndefExp()))))),
                    Vector(
                      WhileDoStmt(
                        LessExp(IntExp(0), IdnExp(IdnUse("n"))),
                        Block(
                          Vector(),
                          Vector(
                            AssignStmt(
                              Vector(IdnExp(IdnUse("temp"))),
                              Vector(IdnExp(IdnUse("res1")))),
                            AssignStmt(
                              Vector(IdnExp(IdnUse("res1"))),
                              Vector(IdnExp(IdnUse("res2")))),
                            AssignStmt(
                              Vector(IdnExp(IdnUse("res2"))),
                              Vector(
                                PlusExp(
                                  IdnExp(IdnUse("temp")),
                                  IdnExp(IdnUse("res1"))))),
                            AssignStmt(
                              Vector(IdnExp(IdnUse("n"))),
                              Vector(MinusExp(IdnExp(IdnUse("n")), IntExp(1))))))),
                      ResultIsStmt(IdnExp(IdnUse("res1"))))))))),
          LetDecl(
            Vector(
              LetProcClause(
                IdnDef("start"),
                Vector(),
                Block(
                  Vector(),
                  Vector(
                    CallStmt(
                      CallExp(
                        IdnExp(IdnUse("writef")),
                        Vector(
                          StringExp(
                            Vector(
                              84,
                              104,
                              101,
                              32,
                              49,
                              48,
                              116,
                              104,
                              32,
                              70,
                              105,
                              98,
                              111,
                              110,
                              97,
                              99,
                              99,
                              105,
                              32,
                              110,
                              117,
                              109,
                              98,
                              101,
                              114,
                              32,
                              105,
                              115,
                              32,
                              37,
                              110,
                              10)),
                          CallExp(
                            IdnExp(IdnUse("fibonacci")),
                            Vector(IdnExp(IdnUse("n")))))))))))))))

  }
}
