from model import Model
from file_reader import FileReader

import argparse

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("-p", "--path", \
        help="path to *-output dirs", \
        type=str, required=False, default=None, nargs="?")
    parser.add_argument("--process", \
        help="process the files - remove stopwords, split, etc.", \
        action="store_true", required=False)
    parser.add_argument("--train", action="store_true",\
        help="trains the Skip gram model")
    parser.add_argument("--context", action="store_true", \
        help="will prompt for word and give its context", required=False)
    args = parser.parse_args()

    reader = FileReader(args.path)
    model = Model(reader)
    print(args.context)
    if args.process:
        reader.preprocess()
    elif args.train:
        model.train(True)
    elif args.context:
        word = input("Enter word: ")
        print(model.predict(word))
    else:
        print("Invalid input")