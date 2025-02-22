import chess
import chess.engine

def parse_perf_debug(file_path):
    """ Parses the perf_debug.txt file and returns a list of positions and their expected successors. """
    with open(file_path, "r") as file:
        lines = file.readlines()

    positions = []
    current_position = None
    generated_positions = []

    for line in lines:
        line = line.strip()
        if line == "NEW":
            if current_position is not None:
                positions.append((current_position, generated_positions))
            current_position = None
            generated_positions = []
        elif current_position is None:
            current_position = line 
        else:
            generated_positions.append(line)  

    if current_position is not None:
        positions.append((current_position, generated_positions))

    return positions

def generate_chess_moves(fen):
    """ Generates correct positions after moves using the chess library, ensuring en passant and castling are handled correctly. """
    board = chess.Board(fen)
    valid_positions = set()

    for move in board.legal_moves:
        board.push(move)

        correct_fen = board.fen(en_passant='fen')

        valid_positions.add(correct_fen)
        board.pop()

    return valid_positions

def validate_positions(file_path):
    """ Checks the correctness of positions from the perf_debug.txt file. """
    positions = parse_perf_debug(file_path)
    errors = []

    for index, (fen, generated_fens) in enumerate(positions):
        valid_positions = generate_chess_moves(fen)

        generated_set = set(generated_fens)
        missing_positions = valid_positions - generated_set
        extra_positions = generated_set - valid_positions

        if missing_positions or extra_positions:
            print(f"Error in position {index+1}: {fen}")
            if missing_positions:
                print(f"  Missing positions:\n    " + "\n    ".join(missing_positions))
            if extra_positions:
                print(f"  Extra incorrect positions:\n    " + "\n    ".join(extra_positions))
            errors.append((fen, missing_positions, extra_positions))
        #else:
           # print(f"Position {index+1} is correct.")

    if not errors:
        print("🎉 All positions are correct!")

if __name__ == "__main__":
    validate_positions("perf_debug.txt")
