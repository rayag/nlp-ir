from gensim.models import Word2Vec
import multiprocessing

class Model:
    def __init__(self, generator):
        self.model = None
        self.generator = generator

    def train(self, first_time = False):
        if first_time:
            self.model = Word2Vec(self.generator, size=500, window=5, min_count=5, \
                negative=15, iter=10, workers=multiprocessing.cpu_count())
        else:
            self.model = Word2Vec.load("word2vec.model")
        self.model.save("word2vec.model")

    def predict(self, word):
        print(f"Predicting {word}")
        if self.model is None:
            self.model = Word2Vec.load("word2vec.model")
        return self.model.similar_by_word(word)