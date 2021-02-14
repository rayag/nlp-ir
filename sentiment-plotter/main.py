import json
import matplotlib.pyplot as plt

MONTHS = ["ян", "февр", "март", "апр", "май", "юни", "юли", "авг", "септ", "окт", "ноем", "дек"]
BASE_DIR = "/Users/sgenchev/irl/nlp-ir"
MEDIA_FILE_NAME_TO_PLOT_PRESENTATION_NAME = {
    "btv": "bTV",
    "dirbg": "Dir.bg",
    "dnevnik": "Дневник",
    "economy": "Economy.bg",
    "manager": "manager.bg",
    "nova": "NOVA",
    "novinibg": "Novini.bg",
    "vesti": "Vesti.bg"}


def month_num_to_text(month_num):
    return MONTHS[int(month_num) - 1]


def parse_json_file(file):
    with open(file) as f:
        return json.load(f)


def get_x_axis(media_json, quarters_count=20):
    return [
        "'" + quarter["periodStart"][2:4] + " " + month_num_to_text(quarter["periodStart"][5:7])
        + "\n'" + quarter["periodEnd"][2:4] + " " + month_num_to_text(quarter["periodEnd"][5:7])

        for quarter in media_json["quarters"][:quarters_count]]


def get_y_positive(media_json, strategy_index, quarters_count=20):
    return [quarter["strategyResults"][strategy_index]["positiveCount"]
            for quarter in media_json["quarters"][:quarters_count]]


def get_y_negative(media_json, strategy_index, quarters_count=20):
    return [quarter["strategyResults"][strategy_index]["negativeCount"]
            for quarter in media_json["quarters"][:quarters_count]]


def get_y_neutral(media_json, strategy_index, quarters_count=20):
    return [quarter["strategyResults"][strategy_index]["neutralCount"]
            for quarter in media_json["quarters"][:quarters_count]]


def get_total_articles_count(media_json, quarters_count=20):
    return sum(map(lambda quarter:
                   quarter["strategyResults"][0]["negativeCount"]
                   + quarter["strategyResults"][0]["positiveCount"]
                   + quarter["strategyResults"][0]["neutralCount"],

                   media_json["quarters"][:quarters_count]))


if __name__ == '__main__':
    for media in MEDIA_FILE_NAME_TO_PLOT_PRESENTATION_NAME.keys():
        parsed_json = parse_json_file(BASE_DIR + "/" + media)
        for quarter in parsed_json["quarters"][:20]:
            assert quarter["strategyResults"][0]["strategy"] == "counting"
            assert quarter["strategyResults"][1]["strategy"] == "ranged"

        x = get_x_axis(parsed_json)
        x.reverse()

        for strategy_index in range(0, 2):
            y_positive = get_y_positive(parsed_json, strategy_index)
            y_positive.reverse()
            y_negative = get_y_negative(parsed_json, strategy_index)
            y_negative.reverse()
            y_neutral = get_y_neutral(parsed_json, strategy_index)
            y_neutral.reverse()

            plt.plot(x, y_positive, "g", label="positive")
            plt.plot(x, y_negative, "r", label="negative")
            plt.plot(x, y_neutral, "y", label="neutral")

            plt.xlabel('quarters')
            plt.ylabel('articles count')

            plt.title("{} ({} articles) with strategy: {}".format(
                MEDIA_FILE_NAME_TO_PLOT_PRESENTATION_NAME[parsed_json["media"]],
                get_total_articles_count(parsed_json),
                "Ranged polarity" if strategy_index == 1 else "Counting"))

            plt.legend()
            plt.show()
