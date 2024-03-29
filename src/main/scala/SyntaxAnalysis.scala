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
 * Parser for the BCPL language.
 */

package webcpl

import org.bitbucket.inkytonik.kiama.parsing.Parsers
import org.bitbucket.inkytonik.kiama.util.Positions

/**
  * Module containing parsers for BCPL.
  */
class SyntaxAnalysis(positions: Positions)
    extends Parsers(positions)
    with LexicalAnalysis {

  import BCPLTree._

  lazy val parser: PackratParser[Program] =
    phrase(program)

  lazy val program: PackratParser[Program] =
    rep1sep(declaration, semiColon) ^^ Program

  lazy val declaration: PackratParser[Declaration] =
    MANIFEST ~> leftBrace ~> rep1sep(manifestEntry, semiColon) <~ rightBrace ^^ ManifestDecl |
      GLOBAL ~> leftBrace ~> rep1sep(globalEntry, semiColon) <~ rightBrace ^^ GlobalDecl |
      STATIC ~> leftBrace ~> rep1sep(staticEntry, semiColon) <~ rightBrace ^^ StaticDecl |
      LET ~> rep1sep(letDeclClause, AND) ^^ LetDecl

  lazy val manifestEntry: PackratParser[ManifestEntry] =
    idndef ~ opt(equal ~> expression) ^^ ManifestEntry

  lazy val staticEntry: PackratParser[StaticEntry] =
    idndef ~ opt(equal ~> expression) ^^ StaticEntry

  lazy val globalEntry: PackratParser[GlobalEntry] =
    idndef ~ opt(colon ~> (integerConst ^^ IntExp)) ^^ GlobalEntry

  lazy val letDeclClause: PackratParser[LetClause] =
    rep1sep(idndef, comma) ~ (equal ~> rep1sep(expression, comma)) ^^ LetVarClause |
      idndef ~ (equal ~> VEC ~> expression) ^^ LetVecClause |
      idndef ~ (leftParen ~> repsep(idndef, comma) <~ rightParen) ~ (equal ~> expression) ^^ LetFnClause |
      idndef ~ (leftParen ~> repsep(idndef, comma) <~ rightParen) ~ (BE ~> statement) ^^ LetProcClause

  /*
   * Statement parsers.
   */

  lazy val statement: PackratParser[Statement] =
    (labdef <~ colon) ~ statement ^^ Labelled |
      (CASE ~> expression) ~ (colon ~> statement) ^^ CaseOf |
      DEFAULT ~> colon ~> statement ^^ Default |
      unlabelledStmt

  // FIXME Replace this stubbed parser.
  /**
    * repeatableStmt | iteratedStmt | testStmt
    */
  lazy val unlabelledStmt: PackratParser[Statement] =
    repeatableStmt | iteratedStmt | testStmt

  // FIXME Add your parsers for weBCPL statements here.
  /**
    * iteratedStmt :
    *    "UNTIL" expression "DO" statement
    *    | "WHILE" expression "DO" statement
    *    | "FOR" idndef "=" expression "TO" expression
    *    ("BY" expression)? "DO" statement
   */
  lazy val iteratedStmt: PackratParser[Statement] =
    (UNTIL ~> expression) ~ (DO ~> statement) ^^ UntilDoStmt |
      (WHILE ~> expression) ~ (DO ~> repeatableStmt) ^^ WhileDoStmt |
      (FOR ~> idndef) ~ (equal ~> expression) ~ (TO ~> expression) ~ opt(BY ~> expression) ~ (DO ~> statement) ^^ ForStmt

  /**
    * case class ForStmt(
    * idn: IdnDef,
    * start: Expression,
    * end: Expression,
    * step: Option[Expression],
    * body: Statement
    * ) extends Statement
    */

  /**
    * testStmt :
    * "TEST" expression "THEN" statement "ELSE" statement
    * | "IF" expression "DO" statement
    * | "UNLESS" expression "DO" statement
   */
  lazy val testStmt: PackratParser[Statement] =
    (TEST ~> expression) ~ (THEN ~> statement) ~ (ELSE ~> statement) ^^ TestThenElseStmt |
      (IF ~> expression) ~ (DO ~> statement) ^^ IfDoStmt |
      (UNLESS ~> expression) ~ (DO ~> statement) ^^ UnlessDoStmt

  /**
    * repeatableStmt :
    * repeatableStmt "REPEAT"
    * | repeatableStmt "REPEATWHILE" expression
    * | repeatableStmt "REPEATUNTIL" expression
    * | simpleStmt
    */
  lazy val repeatableStmt: PackratParser[Statement] =
    repeatableStmt <~ REPEAT ^^ RepeatStmt |
      repeatableStmt ~ (REPEATWHILE ~> expression) ^^ RepeatWhileStmt |
      repeatableStmt ~ (REPEATUNTIL ~> expression) ^^ RepeatUntilStmt |
      simpleStmt

  /**
    * simpleStmt :
    * (expression ",")* expression ":=" (expression ",")* expression
    * | callExp
    * | "BREAK" | "LOOP" | "ENDCASE" | "RETURN" | "FINISH"
    * | "GOTO" labuse
    * | "RESULTIS" expression
    * | "SWITCHON" expression "INTO" blockStmt
    * | blockStmt
    */
  lazy val simpleStmt: PackratParser[Statement] =
    rep1sep(expression,comma) ~ (assign ~> rep1sep(expression,comma)) ^^ AssignStmt |
      callExp ^^ CallStmt |
      BREAK ^^^ BreakStmt() |
      LOOP ^^^ LoopStmt() |
      ENDCASE ^^^ EndCaseStmt() |
      RETURN ^^^ ReturnStmt() |
      FINISH ^^^ FinishStmt() |
      GOTO ~> labuse ^^ GotoStmt |
      RESULTIS ~> expression ^^ ResultIsStmt |
      (SWITCHON ~> expression) ~ (INTO ~> blockStmt) ^^ SwitchOnStmt |
      blockStmt

  /**
    * blockStmt : "{" (declaration ";")* (statement ";")* statement "}"
    */
  lazy val blockStmt: PackratParser[Block] =
    leftBrace ~> repsep(declaration,semiColon) ~ rep1sep(statement, semiColon) <~ rightBrace ^^ Block

  /*
   * Expression parsers.
   */

  /**
    * Top level expression parser, parse `VALOF` and `TABLE` expressions.
    */
  lazy val expression: PackratParser[Expression] =
    VALOF ~> statement ^^ ValofExp |
      TABLE ~> rep1sep(expression, comma) ^^ TableExp |
      condExp

  /**
    * Level 1, parse if expressions `->`.
    */
  // FIXME Replace this stubbed parser
  lazy val condExp: PackratParser[Expression] =
    eqvXorExp ~ (rightArrow ~> condExp) ~ (comma ~> condExp) ^^ IfExp |
      eqvXorExp

  // FIXME Add your expression parsers for levels 1-6 of the precedence hierarchy here.

  lazy val eqvXorExp: PackratParser[Expression] =
    (eqvXorExp <~ EQV) ~ orExp ^^ EqvExp |
      (eqvXorExp <~ XOR) ~ orExp ^^ XorExp |
      orExp

  lazy val orExp: PackratParser[Expression] =
    (orExp <~ pipe) ~ andExp ^^ OrExp |
      andExp

  lazy val andExp: PackratParser[Expression] =
    (andExp <~ apersand) ~ notExp ^^ AndExp |
      notExp

  lazy val notExp: PackratParser[Expression] =
    NOT ~> notExp ^^ NotExp |
      shiftExp

  lazy val shiftExp: PackratParser[Expression] =
    (shiftExp <~ shiftLeft) ~ relExp ^^ ShiftLeftExp |
      (shiftExp <~ shiftRight) ~ relExp ^^ ShiftRightExp |
      relExp
  /**
    * Level 7, parse relational expressions `~=`, `=`, `>=`, `<=`...
    *
    * This is slightly nonstandard because in BCPL we can write relational
    * expressions like `a <= b < c > d` which in other languages might be
    * written as `a <= b & b < c & c > d`.
    */
  lazy val relExp: PackratParser[Expression] =
    rep1(
      addExp ~
        (notEqual ^^^ NotEqualExp |
          lessOrEqual ^^^ LessOrEqualExp |
          greaterOrEqual ^^^ GreaterOrEqualExp |
          equal ^^^ EqualExp |
          less ^^^ LessExp |
          greater ^^^ GreaterExp)
    ) ~ addExp ^^ {
      case v ~ t =>
        (v zip (v.tail.map(_._1) :+ t))
          .map { case ((l ~ rel), r) => rel(l, r) }
          .reduceLeft(AndExp)
    } | addExp

  /**
    * Level 8, parse additive operator expressions, that is those involving
    * binary `-` and `+`.
    */
  // FIXME Replace this stubbed parser

  /**
    * | expression "+" expression
    * | expression "-" expression
    * | "-" expression
    * | "+" expression
    * | "ABS" expression
    * | expression "*" expression
    * | expression "/" expression
    * | expression "MOD" expression
    * | "!" expression
    * | "%" expression
    * | "@" expression
    * | expression "!" expression
    * | expression "%" expression
    */
  lazy val addExp: PackratParser[Expression] =
    (addExp <~ plus) ~ unaryAddExp ^^ PlusExp |
      (addExp <~ minus) ~ unaryAddExp ^^ MinusExp |
      unaryAddExp

  // FIXME Add your expression parsers for levels 8-12 of the precedence hierarchy here.

  lazy val unaryAddExp: PackratParser[Expression] =
    unaryMinus ~> unaryAddExp ^^ NegExp |
      unaryPlus ~> unaryAddExp |
      ABS ~> unaryAddExp ^^ AbsExp |
      multiExp

  lazy val multiExp: PackratParser[Expression] =
    (multiExp <~ star) ~ addressExp ^^ StarExp |
      (multiExp <~ slash) ~ addressExp ^^ SlashExp |
      (multiExp <~ MOD) ~ addressExp ^^ ModExp |
      addressExp

  lazy val addressExp: PackratParser[Expression] =
    unaryPling ~> addressExp ^^ UnaryPlingExp |
      unaryPercent ~> addressExp ^^ UnaryBytePlingExp |
      at ~> addressExp ^^ AddrOfExp |
      vecExp

  lazy val vecExp: PackratParser[Expression] =
    (vecExp <~ pling) ~ primaryExp ^^ BinaryPlingExp |
      (vecExp <~ percent) ~ primaryExp ^^ BinaryBytePlingExp |
      primaryExp
  /**
    * Level 13, parse primary expressions, that is function calls, identifiers,
    * bracketed expressions, and literal constants.
    */
  lazy val primaryExp: PackratParser[Expression] =
    callExp | elemExp

  lazy val callExp: PackratParser[CallExp] =
    (callExp | elemExp) ~ (leftParen ~> repsep(expression, comma) <~ rightParen) ^^ CallExp

  // PRO TIP: Place parsers that match longer initial segments earlier in an alternation.

  /*
   * If two clauses of an alternation `|` can match the same text, then place the one
   * that matches longer initial segments first. This ensures that the longest possible
   * match is preferred.
   */

  lazy val elemExp: PackratParser[Expression] =
    leftParen ~> expression <~ rightParen |
      TRUE ^^^ TrueExp() |
      FALSE ^^^ FalseExp() |
      question ^^^ UndefExp() |
      integerConst ^^ IntExp |
      charConst ^^ ChrExp |
      stringConst ^^ StringExp |
      idnuse ^^ IdnExp

  lazy val idndef: PackratParser[IdnDef] =
    identifier ^^ IdnDef

  lazy val idnuse: PackratParser[IdnUse] =
    identifier ^^ IdnUse

  lazy val labdef: PackratParser[LabDef] =
    identifier ^^ LabDef

  lazy val labuse: PackratParser[LabUse] =
    identifier ^^ LabUse
}
