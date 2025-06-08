from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field, field_validator
from typing import List, Literal, Tuple
import random, itertools

app = FastAPI(title="Gomoku Wi-Fi Bot")

DIRECTIONS = [(1, 0), (0, 1), (1, 1), (1, -1)]
Board = List[List[str]]
Symbol = Literal["X", "O"]
bot_symbol: str = "X"

class BoardIn(BaseModel):
    board: Board = Field(..., min_items=14, max_items=14)

class SymbolIn(BaseModel):
    chosenSymbol: Literal["X", "O"]

    @field_validator("chosenSymbol")
    @classmethod
    def must_be_upper(cls, v: str) -> str:
        return v.upper()

class Move(BaseModel):
    row: int
    col: int
    symbol: Symbol

SIZE = 14

@app.get("/game/first-move")
def first_move() -> dict:
    """
    Return bot's name and its 3-stone opening (2 x X, 1 x O).
    """
    
    r1 = random.randint(0, SIZE - 1)
    c1 = random.randint(0, SIZE - 1)

    neighbours: List[Tuple[int, int]] = []
    if r1 > 0:             neighbours.append((r1 - 1, c1))
    if r1 < SIZE - 1:      neighbours.append((r1 + 1, c1))
    if c1 > 0:             neighbours.append((r1, c1 - 1))
    if c1 < SIZE - 1:      neighbours.append((r1, c1 + 1))
    r2, c2 = random.choice(neighbours)

    while True:
        r3 = random.randint(0, SIZE - 1)
        c3 = random.randint(0, SIZE - 1)
        if (r3, c3) not in {(r1, c1), (r2, c2)}:
            break

    moves = [
        Move(row=r1, col=c1, symbol="X"),
        Move(row=r2, col=c2, symbol="X"),
        Move(row=r3, col=c3, symbol="O"),
    ]
    
    return {"opponentName": "WifiBot", "moves": [m.model_dump() for m in moves]}

@app.post("/game/announce-symbol")
def announce_symbol(data: SymbolIn) -> dict:
    """
    Client tells us which symbol *it* will play.
    We reply with the opposite symbol and remember it globally.
    """

    if data.chosenSymbol not in ("X", "O"):
        raise HTTPException(400, "chosenSymbol must be 'X' or 'O'")

    bot_symbol = "O" if data.chosenSymbol == "X" else "X"

    return {"botSymbol": bot_symbol}

@app.post("/game/choose")
def choose_symbol(data: BoardIn) -> dict:
    """
    Pick the symbol with **fewer** occurrences on the current board
    (very naive - prevents obvious imbalance).
    """
    tmp = random.randint(0, 1)
    bot_symbol = "X" if tmp == 0 else "O"
    
    return {"chosenSymbol": bot_symbol}

@app.post("/game/next-move")
def next_move(data: BoardIn) -> dict:
    """
    1. Play the immediate winning move, if any.
    2. Otherwise block opponent’s immediate win.
    3. Otherwise play the highest-scoring square (naïve heuristic).
    """
    board = data.board
    empties = [(r, c) for r, c in itertools.product(range(SIZE), range(SIZE))
               if board[r][c] == ""]

    if not empties:
        raise HTTPException(409, "Board is full")

    opponent_symbol = "O" if bot_symbol == "X" else "X"

    for r, c in empties:
        if would_make_five(board, r, c, bot_symbol):
            return {"row": r, "col": c}

    for r, c in empties:
        if would_make_five(board, r, c, opponent_symbol):
            return {"row": r, "col": c}

    scored = [(score_cell(board, r, c, bot_symbol), r, c) for r, c in empties]
    max_score = max(s for s, _, _ in scored)
    best_moves = [(r, c) for s, r, c in scored if s == max_score]
    row, col = random.choice(best_moves)

    return {"row": row, "col": col}

def in_bounds(r: int, c: int) -> bool:
    return 0 <= r < SIZE and 0 <= c < SIZE

def would_make_five(board: Board, r: int, c: int, sym: str) -> bool:
    """Return True if placing `sym` at (r,c) yields 5-in-row in any direction."""
    for dr, dc in DIRECTIONS:
        count = 1

        i = 1
        while in_bounds(r + dr * i, c + dc * i) and board[r + dr * i][c + dc * i] == sym:
            count += 1
            i += 1

        i = 1
        while in_bounds(r - dr * i, c - dc * i) and board[r - dr * i][c - dc * i] == sym:
            count += 1
            i += 1
        if count >= 5:
            return True
    return False

def score_cell(board: Board, r: int, c: int, sym: str) -> int:
    """Very small heuristic: longest contiguous run of `sym` that this cell extends."""
    best = 0
    for dr, dc in DIRECTIONS:
        run = 1

        i = 1
        while in_bounds(r + dr * i, c + dc * i) and board[r + dr * i][c + dc * i] == sym:
            run += 1
            i += 1

        i = 1
        while in_bounds(r - dr * i, c - dc * i) and board[r - dr * i][c - dc * i] == sym:
            run += 1
            i += 1
        best = max(best, run)
    return best
