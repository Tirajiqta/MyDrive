"""
Run this once to seed MongoDB with pizza menu and intents.
Usage: python seed_db.py
"""
import pymongo

client = pymongo.MongoClient("mongodb://localhost:27017/")
db = client["pizza_restaurant"]

# ── Clear old data ──────────────────────────────────────────────────────────
db["intents"].drop()
db["pizzas"].drop()
db["unknown_queries"].drop()       # learning collection
db["order_history"].drop()         # learning collection

# ── Pizza menu ──────────────────────────────────────────────────────────────
pizzas = [
    {
        "name": "Margherita",
        "ingredients": ["tomato sauce", "mozzarella", "fresh basil"],
        "sizes": {"small": 7.99, "medium": 10.99, "large": 13.99},
        "description": "The classic Italian original — simple and timeless."
    },
    {
        "name": "Quattro Formaggi",
        "ingredients": ["mozzarella", "gorgonzola", "parmesan", "fontina"],
        "sizes": {"small": 9.99, "medium": 12.99, "large": 15.99},
        "description": "Four-cheese heaven for the true cheese lover."
    },
    {
        "name": "Pepperoni",
        "ingredients": ["tomato sauce", "mozzarella", "pepperoni"],
        "sizes": {"small": 8.99, "medium": 11.99, "large": 14.99},
        "description": "A crowd-pleaser loaded with spicy pepperoni slices."
    },
    {
        "name": "BBQ Chicken",
        "ingredients": ["bbq sauce", "mozzarella", "grilled chicken", "red onion", "coriander"],
        "sizes": {"small": 9.49, "medium": 12.49, "large": 15.49},
        "description": "Smoky BBQ sauce with tender grilled chicken."
    },
    {
        "name": "Veggie Supreme",
        "ingredients": ["tomato sauce", "mozzarella", "bell peppers", "mushrooms", "olives", "red onion"],
        "sizes": {"small": 8.49, "medium": 11.49, "large": 14.49},
        "description": "Garden-fresh vegetables on a classic tomato base."
    },
    {
        "name": "Hawaiian",
        "ingredients": ["tomato sauce", "mozzarella", "ham", "pineapple"],
        "sizes": {"small": 8.49, "medium": 11.49, "large": 14.49},
        "description": "The controversial classic — ham and sweet pineapple."
    },
    {
        "name": "Diavola",
        "ingredients": ["tomato sauce", "mozzarella", "spicy salami", "chilli flakes", "fresh chilli"],
        "sizes": {"small": 9.49, "medium": 12.49, "large": 15.49},
        "description": "Fiery hot for those who like it spicy."
    },
    {
        "name": "Prosciutto e Funghi",
        "ingredients": ["tomato sauce", "mozzarella", "prosciutto", "mushrooms"],
        "sizes": {"small": 9.99, "medium": 12.99, "large": 15.99},
        "description": "Italian ham and earthy mushrooms — a classic pairing."
    },
    {
        "name": "Truffle Mushroom",
        "ingredients": ["truffle oil", "mozzarella", "mixed mushrooms", "parmesan", "fresh thyme"],
        "sizes": {"small": 11.99, "medium": 14.99, "large": 17.99},
        "description": "Luxurious truffle oil with a wild mushroom medley."
    },
    {
        "name": "Meat Feast",
        "ingredients": ["tomato sauce", "mozzarella", "pepperoni", "beef mince", "bacon", "spicy sausage"],
        "sizes": {"small": 10.99, "medium": 13.99, "large": 16.99},
        "description": "Every meat you love, piled high on one pizza."
    },
]

db["pizzas"].insert_many(pizzas)
print(f"✅ Inserted {len(pizzas)} pizzas.")

# ── Intents ──────────────────────────────────────────────────────────────────
intents = [
    {
        "intent": "Order_Pizza",
        "phrases": [
            "I want to order a pizza",
            "Can I get a pizza",
            "I'd like to order",
            "I want a pizza",
            "order pizza please",
            "let me get a pizza",
            "I would like to order a pizza",
            "get me a pizza",
            "I'll have a pizza",
        ],
        "responses": [],   # handled by state machine
        "hit_count": 0,
    },
    {
        "intent": "Show_Menu",
        "phrases": [
            "what pizzas do you have",
            "show me the menu",
            "what's on the menu",
            "what can I order",
            "what do you serve",
            "menu please",
            "what pizzas are available",
            "list your pizzas",
            "what are my options",
        ],
        "responses": [],   # handled dynamically
        "hit_count": 0,
    },
    {
        "intent": "Pizza_Info",
        "phrases": [
            "what is on the margherita",
            "what are the ingredients",
            "tell me about the pizza",
            "what does the quattro formaggi have",
            "what toppings are on that",
            "describe the pizza",
            "what comes on the hawaiian",
        ],
        "responses": [],
        "hit_count": 0,
    },
    {
        "intent": "Greeting",
        "phrases": [
            "hello",
            "hi there",
            "hey",
            "good morning",
            "good evening",
            "howdy",
            "what's up",
        ],
        "responses": [
            "Hey there! Welcome to Mario's Pizza 🍕 How can I help you today?",
            "Hello! Ready to order the best pizza in town?",
            "Hi! Great to see you. Fancy a pizza today?",
        ],
        "hit_count": 0,
    },
    {
        "intent": "Thanks",
        "phrases": [
            "thank you",
            "thanks",
            "cheers",
            "appreciate it",
            "thank you so much",
        ],
        "responses": [
            "You're welcome! Enjoy your pizza 🍕",
            "Happy to help! Let me know if you need anything else.",
            "No problem at all!",
        ],
        "hit_count": 0,
    },
    {
        "intent": "Goodbye",
        "phrases": [
            "bye",
            "goodbye",
            "see you later",
            "take care",
            "ciao",
        ],
        "responses": [
            "Goodbye! Come back soon 🍕",
            "See you next time! Ciao!",
            "Take care! Your pizza awaits 🍕",
        ],
        "hit_count": 0,
    },
    {
        "intent": "Opening_Hours",
        "phrases": [
            "what time do you open",
            "what are your opening hours",
            "are you open",
            "when do you close",
            "what time do you close",
            "are you open right now",
        ],
        "responses": [
            "We're open Monday–Saturday 11am–11pm and Sunday 12pm–10pm 🕐",
            "Our hours are Mon–Sat 11am to 11pm, and Sunday 12pm to 10pm.",
        ],
        "hit_count": 0,
    },
    {
        "intent": "Delivery_Time",
        "phrases": [
            "how long does delivery take",
            "how long will it take",
            "when will my pizza arrive",
            "delivery time",
            "how fast is delivery",
        ],
        "responses": [
            "Delivery usually takes 30–45 minutes depending on your location 🛵",
            "Expect your pizza in about 30–45 minutes!",
        ],
        "hit_count": 0,
    },
]

db["intents"].insert_many(intents)
print(f"✅ Inserted {len(intents)} intents.")
print("✅ Created 'unknown_queries' and 'order_history' collections (auto on first insert).")
print("\nDatabase seeded successfully! You can now run app.py.")
