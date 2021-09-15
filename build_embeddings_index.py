import json
import sys
import numpy as np
from sentence_transformers import SentenceTransformer
from embeddings_index import EmbeddingsIndex

dir=sys.argv[1]
filename = sys.argv[2]
outname = sys.argv[3]

myEI = EmbeddingsIndex(dir, filename, outname)
myEI.build_index()

