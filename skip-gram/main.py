from model import Model
from file_reader import FileReader
from tf_idf import TfIdf
from timeit import default_timer as timer
import heapq
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
    parser.add_argument("--search", action="store_true", \
        help="will prompt for question and give relevant results according to tf-idf", \
        required=False)
    args = parser.parse_args()

    start = timer()

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
    elif args.search:
        query = input("Enter question: ")
        tfidf = TfIdf(reader)
        closest = tfidf.get_k_closest(reader.preprocess_input(query))
        while True:
            try:
                cos_dist, art_path = heapq.heappop(closest)
                if "dnevnik" in art_path:
                    continue
                title, summary, body = reader.get_clean_article(art_path)
                print(title, cos_dist)
                print(summary)
                print(body)
                print('\n\n')
            except:
                break
    else:
        print("Invalid input")
    end = timer()
    print(f"Took: {end - start}")
