from model import Model
from file_reader import FileReader
from tf_idf import TfIdf
from timeit import default_timer as timer
import heapq
import argparse
from gensim.corpora.dictionary import Dictionary
from gensim.models.tfidfmodel import TfidfModel
from gensim.matutils import corpus2dense, corpus2csc
from gensim import corpora, models, matutils
from sklearn.cluster import KMeans
import pickle
import numpy
import matplotlib.pyplot as plt
from media_dirs import Media
import json
import datetime
import numpy as np 
import matplotlib.pyplot as plt

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
    parser.add_argument("--cluster", action="store_true", \
        help="calculates and prints clusters by year", required=False)
    parser.add_argument("--quarter", action="store_true", \
        help="calculates and prints clusters by quarter and by media", required=False)
    parser.add_argument("--search", action="store_true", \
        help="will prompt for question and give relevant results according to tf-idf", \
        required=False)
    args = parser.parse_args()

    start = timer()

    reader = FileReader(args.path)
    model = Model(reader)
    
    if args.process:
        reader.preprocess()
    elif args.train:
        print("training model...")
        model.train(False)
    elif args.context:
        word = input("Enter word: ")
        pred = model.predict(word)
        x = [t[0] for t in pred]
        y = [t[1] for t in pred]

        fig, ax = plt.subplots()    
        width = 0.75 # the width of the bars 
        ind = np.arange(len(y))  # the x locations for the groups
        ax.barh(ind, y, color="green")
        ax.set_yticks(ind+width/2)
        ax.set_yticklabels(x, minor=False)
        plt.title('Контест за думата ' + word)
        plt.xlabel('x')
        plt.ylabel('y')      
        plt.show()
    elif args.search:
        query = input("Enter question: ")
        tfidf = TfIdf(reader)
        closest = tfidf.get_k_closet_to_query(reader.preprocess_input(query))
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
    elif args.cluster:
        reader = FileReader()    
        for year in range(2015, 2022):
            print(f"Year: {year}")
            dictionary = Dictionary(reader.get_by_year(year))
            num_docs = dictionary.num_docs
            print(f"Num docs {num_docs}")
            num_terms = len(dictionary.keys())
            corpus_bow = [dictionary.doc2bow(text) for text in reader.get_by_year(year)]
            dictionary.save("dictionary" + str(year) + ".model")
            tfidf = TfidfModel(corpus_bow)
            tfidf.save("tfidf"+ str(year) + ".model")
            corpus_tfidf = tfidf[corpus_bow]
            sparse_corpus_tfidf = matutils.corpus2csc(corpus_tfidf).transpose()
            kmeans = KMeans(n_clusters=10)
            kmeans.fit_predict(sparse_corpus_tfidf)
            for j in range(10):
                x = list(matutils.dense2vec(kmeans.cluster_centers_[j]))
                x.sort(key=lambda tup: tup[1], reverse=True)
                print(f"Cluster {j}")
                print([dictionary[x[i][0]] for i in range(10)])
            print()
            print()
    elif args.quarter:
        quarters_base_dir = '/home/lt38092/crawled_news/quarters/'
        reader = FileReader()
        media = Media.VESTI
        parsed = json.loads(open(quarters_base_dir + media.value, 'r').read())
        print(f"{media.value.upper()}")
        for q in parsed['quarters']:
            startDate = datetime.datetime.strptime(q["periodStart"], '%Y-%m-%d')
            endDate = datetime.datetime.strptime(q['periodEnd'], '%Y-%m-%d')
            print(f"Start: {startDate.date()}  End: {endDate.date()}")
            dictionary = Dictionary(reader.get_by_media_and_in_period(media, startDate, endDate))
            num_docs = dictionary.num_docs
            if num_docs <= 5:
                continue
            print(f"Num docs {num_docs}")
            num_terms = len(dictionary.keys())
            corpus_bow = [dictionary.doc2bow(text) for text in reader.get_by_media_and_in_period(media, startDate, endDate)]
            tfidf = TfidfModel(corpus_bow)
            corpus_tfidf = tfidf[corpus_bow]
            sparse_corpus_tfidf = matutils.corpus2csc(corpus_tfidf).transpose()
            kmeans = KMeans(n_clusters=5)
            kmeans.fit_predict(sparse_corpus_tfidf)
            for j in range(5):
                x = list(matutils.dense2vec(kmeans.cluster_centers_[j]))
                x.sort(key=lambda tup: tup[1], reverse=True)
                print(f"Cluster {j}")
                print([dictionary[x[i][0]] for i in range(min(10, len(x)))])
            print()
        print(f"End {media.value}")
    else:
        print("Invalid input")
    end = timer()
    print(f"Took: {end - start}")
