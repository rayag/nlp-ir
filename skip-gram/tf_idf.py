import json
import os
import numpy as np
import heapq
import collections
from numpy.linalg import norm

INDEX_DIR_PATH = "/home/lt38092/crawled_news/index/"

# these 3 must sum up to 1
# weights of each component of an article
TITLE_WEIGHT = 0.45
SUMMARY_WEIGHT = 0.35
BODY_WEIGHT = 0.2

class TfIdf:
    def __init__(self, reader):
        self.reader = reader
        self.df = {}
        self.docs_count = 0
        self.vocabulary_size = 0
        self.get_df_from_files()
        # self.build_index()
        # self.write_indices_to_files()

    def build_index(self):
        self.docs_count = 0
        i = self.reader.iter_filename()
        while True:
            try:
                self.docs_count += 1
                tokens, fname = next(i)
                self.add_tokens_to_df(tokens, fname)
            except StopIteration:
                break
    
    def add_tokens_to_df(self, tokens, filename):
        for t in tokens:
            try:
                self.df[t].add(filename)
            except:
                self.df[t] = {filename}

    def get_df_from_files(self):
        i = 0
        with (open(os.path.join(INDEX_DIR_PATH, 'info'), 'r', encoding='utf8')) as f:
            info = json.loads(f.read())
            self.docs_count = info["docsCount"]

        for f in os.listdir(INDEX_DIR_PATH):
            if f == "info":
                continue
            data = json.load(open(INDEX_DIR_PATH + f, 'r'))
            self.df[f] = (i, data["docCount"])
            i += 1
        self.vocabulary_size = len(self.df)

    def get_k_closest(self, query_tokens, k = 15):
        closest_docs = list()
        query_unique_words = self.unique_words_counts(query_tokens)
        query_words_count = len(query_tokens)
        query_vector = self.get_vector_from_tokens(query_tokens, \
            query_unique_words, query_words_count)

        docs_set = self.get_docs(query_unique_words.keys())

        for doc_path in docs_set:
            doc_vector = self.document_vector(doc_path)
            cos_sim = np.inner(query_vector, doc_vector) / (norm(query_vector) * norm(doc_vector))
            if len(closest_docs) < k:
                heapq.heappush(closest_docs, (cos_sim, doc_path))
            else:
                heapq.heappushpop(closest_docs, (cos_sim, doc_path))
        return closest_docs

    def get_k_closet_to_query(self, query_tokens, k = 15):
        closest_docs = list()
        query_unique_tokens = list(set(query_tokens))
        query_tf_idf = self.get_tf_idf(query_unique_tokens, query_tokens)
        docs_set = self.get_docs(query_unique_tokens)

        for doc_path in docs_set:
            doc_vector = self.article_tf_idf(query_unique_tokens ,doc_path)
            cos_sim = np.inner(query_tf_idf, doc_vector) / (norm(query_tf_idf) * norm(doc_vector))
            if len(closest_docs) < k:
                heapq.heappush(closest_docs, (cos_sim, doc_path))
            else:
                heapq.heappushpop(closest_docs, (cos_sim, doc_path))
        return closest_docs


    def get_tf_idf(self, unique_tokens, tokens):
        counts = collections.Counter(tokens)
        n = len(unique_tokens)
        tf = np.zeros(n, dtype=float)
        for i in range(n):
            if unique_tokens[i] in counts:
                tf[i] = counts[unique_tokens[i]]
        tf = tf / (len(tokens) + 1)

        tf_idf = tf
        for i in range(n):
            df = 1
            if unique_tokens[i] in self.df:
                _, df = self.get_df_idx(unique_tokens[i])
            idf = 1 + np.log(self.docs_count / df)
            tf_idf[i] *= idf
        return tf_idf 
                
    def article_tf_idf(self, unique_tokens, doc_path):
        title, summary, body = self.reader.read_article(doc_path)
        title_tf_idf = self.get_tf_idf(unique_tokens, title)
        summary_tf_idf = self.get_tf_idf(unique_tokens, summary)
        body_tf_idf = self.get_tf_idf(unique_tokens, body)
        return TITLE_WEIGHT * title_tf_idf + SUMMARY_WEIGHT * summary_tf_idf + BODY_WEIGHT * body_tf_idf

    def get_docs(self, tokens):
        result = set()
        for token in tokens:
            docs = self.reader.get_property_from_json_file(INDEX_DIR_PATH + '/' + token, 'docIds')
            for doc in docs:
                result.add(doc)
        return result

    def document_vector(self, doc_path):
        title, summary, body = self.reader.read_article(doc_path)
        words_count = len(title) + len(summary) + len(body)
        unique_words = self.unique_words_counts([*title, *summary, *body])
        titleVec = self.get_vector_from_tokens(title, unique_words, words_count)
        summaryVec = self.get_vector_from_tokens(summary, unique_words, words_count)
        bodyVec = self.get_vector_from_tokens(body, unique_words, words_count)
        return TITLE_WEIGHT * titleVec + SUMMARY_WEIGHT * summaryVec + BODY_WEIGHT * bodyVec

    def get_vector_from_tokens(self, tokens, unique_words, words_count):
        vector = np.zeros(self.vocabulary_size, dtype=float)
        token_counts = self.unique_words_counts(tokens)
        for token, cnt in token_counts.items():
            idx, df = self.get_df_idx(token)
            if idx is not None:
                tf = cnt / words_count
                idf = np.log(self.docs_count / (df + 1))
                vector[idx] = tf * idf            
        return vector

    def get_df_idx(self, word):
        if word in self.df:
            return self.df[word]
        else: 
            return None, 1

    def unique_words_counts(self, tokens):
        result = dict()
        for token in tokens:
            if token in result:
                result[token] += 1
            else:
                result[token] = 1
        return result

    def write_indices_to_files(self):
        with (open(os.path.join(INDEX_DIR_PATH, 'info'), 'w', encoding='utf8')) as f:
            print(f)
            json.dump({ "docsCount": self.docs_count }, f, ensure_ascii=False)

        for word, docs in self.df.items():
            if len(docs) <= 10:
                continue
            json_obj = {}
            json_obj["docCount"] = len(docs)
            json_obj["docIds"] = list(docs)
            with (open(os.path.join(INDEX_DIR_PATH, word), 'w', encoding='utf8')) as f:
                    f.write("")
                    json.dump(json_obj, f, ensure_ascii=False)
