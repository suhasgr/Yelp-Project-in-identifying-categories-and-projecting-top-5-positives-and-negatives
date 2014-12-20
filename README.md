Yelp-Project-in-identifyimg-categories-and-projecting-top-5-positives-and-negatives
===================================================================================

Yelp - project in Identifying categories for business and finding top positives/negatives about business


In this project, you will have chance to practice the information retrieval and text mining methodologies and
algorithms you learned from Z534 course. For this project, we will need to use Yelp data (text data + numeric
data) to investigate some interesting problems. To access and download Yelp data, please go to:
http://www.yelp.com/dataset_challenge In this data set, you will have access to
• 42,153 businesses
• 320,002 business attributes
• 31,617 check-in sets
• 252,898 users
• 955,999 edge social graph
• 403,210 tips
• 1,125,458 reviews
Where tips and reviews are text base, and all the data are saved in JSON file. In order to extract information
from JSON files, you will need to use some JSON packages.
Task
1
-­‐
find
categories
of
each
business:
Category information is critical for Yelp, for instance:
• "categories": ["Shopping", "Drugstores", "Beauty & Spas", "Food", "Convenience Stores", "Cosmetics
& Beauty Supply"]
• Or "categories": ["Indian", "Restaurants"]
• Or "categories": ["Steakhouses", "Restaurants”]
Category information can be important for business recommendation. However, not all the businesses have high
quality category metadata. For instance, if some business is just labeled as “Restaurant”, the label is not very
2
informative. In this task, you will need to design and implement algorithms to predict the categories of
businesses by using data mining, text mining or information retrieval algorithms.
For example: from information retrieval viewpoint, each category can be represented by a query, and we can
use this query to search in the “review” or “tip” of a business (you may need to build Lucene index first. Each
business is a document, and “review” and “tip” are two separate fields). If ranking score is larger than a
threshold, this business belongs to the target category.
For “Indian” category, for instance, some exemplar queries could be: review:”Indian” OR tip:”Indian” or
(review:”Indian” OR tip:”Indian”)^1.5 (review:”curry” OR review:”Pradesh”). Note that, this method has
a number of limitations, and it can be challenging to generate a useful query for each category.
Alternatively, you can use Machine Learning to solve this problem. Each category, then, can be the class label
for the target business, and each business attribute or each word in the “review” or “tip” can be used as a
feature. If you use this approach, you may want to use Weka, http://www.cs.waikato.ac.nz/ml/weka/ an open
source machine learning package implemented in Java.
Task Requirement:
1. You will need to clearly propose the solution (method and algorithm) for this task. For instance, in the
final report, you need to address which algorithm you plan to use, why you choose this method, what is
the advantage/limitation for this proposed solution, what is the key parameter(s) for this method, etc. Of
course, you can propose more than one solution, and comparing different methods can be interesting.
2. Evaluation. How to evaluate your proposed method or different parameter settings? To solve this
problem, you will need to clearly propose what are the evaluation metrics, i.e., precision, recall, Fmeasure,
MAP, precision at rank… Note that different methods may need different kinds of evaluation
metrics.
3. Summarize your findings. Based on your experiment and evaluation, please summarize your findings.
And I would suggest you propose future work to further enhance the algorithm performance.
4. For this task, you will need to use textual information, i.e., the text information in “review” and “tip”.

Task
2

It isn very cumbersome to read all reviews and come up with conclusion that what is good in the business or what is bad 
about it. More over the review are more subjective. 
So we have implemented to know top five positives and top five negatives about the business using Lucene, StanforNLP 
and other frameworks.


