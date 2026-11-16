---
title: AI Chatbots for Customer Service
pdf: AI at Work - Chatbots.pdf
brand: AI in Day-to-Day Work
docid: AI-03
version: 1.0
effective: November 2026
appliesTo: Teams that build or run chatbots for customers or employees
disclaimer: Sample document created for the KnowGauge demo. It gives general information only; check your organisation's own rules before using AI tools at work.
---

# AI Chatbots for Customer Service

AI chatbots answer customer and employee questions in a chat window, around the clock. This guide covers four
subjects: how they work, good uses, risks and limitations, and privacy and data security.

## 1. How AI chatbots work

### 1.1 Language models in a conversation

- Modern chatbots use a **large language model (LLM)** that reads the whole **conversation so far** and writes the
  next reply.
- A hidden **system prompt** tells the model its role, tone and rules, for example "answer only questions about our
  products".

### 1.2 Answers grounded in company documents

- With **retrieval-augmented generation (RAG)**, the chatbot first **searches the company's documents** for passages
  related to the question and then answers using those passages.
- Documents are split into **chunks** and stored as **embeddings**: lists of numbers that represent meaning, so that
  similar questions and passages are found even when the words differ.
- RAG lets the chatbot answer from **current** company information without retraining the model.

### 1.3 Tools and hand-over

- Chatbots can call **tools**, such as looking up an order status or booking an appointment.
- A good chatbot **hands over to a human** agent when it cannot help or when the customer asks for a person.

## 2. Good uses of AI chatbots

### 2.1 Frequent questions

- Answering **frequently asked questions** about opening hours, prices, delivery and returns.
- Guiding customers through simple tasks, such as **resetting a password**.

### 2.2 Internal help desks

- Answering **employee questions** about HR policies, IT procedures and travel rules from the company handbook.
- Helping new employees find the right document or contact during **onboarding**.

### 2.3 Support for human agents

- **Suggesting replies** that a human agent checks and sends.
- **Summarising** a long conversation when a case is handed over to another agent.

## 3. Risks and limitations of AI chatbots

### 3.1 Wrong answers

- A chatbot can **hallucinate** an answer that sounds official but is not in any company document.
- Courts and regulators have held companies **responsible for what their chatbot promises** customers, for example a
  refund the policy does not allow.
- Limit the chatbot to answering from **approved sources**, and let it say "I don't know".

### 3.2 Misuse

- In **prompt injection**, a user writes instructions that try to make the chatbot ignore its rules or reveal its
  system prompt.
- Test the chatbot with such attempts before launch and keep **logs** to find problems.

### 3.3 Customer experience

- Customers become frustrated when a chatbot **loops** or cannot reach a human.
- Measure **resolution rate** and **customer satisfaction**, not only the number of conversations.

## 4. Privacy and data security with AI chatbots

### 4.1 Transparency

- Users must know they are **talking to an AI**, not a person. The **EU AI Act** requires that people are informed
  when they interact with an AI system.
- Tell users what happens with their **messages** and how long they are kept.

### 4.2 Personal data in conversations

- Customers often type **personal data** such as names, addresses or order numbers into a chat.
- Under the **GDPR**, collect only what is needed, keep conversation logs only as long as necessary, and protect
  them like other customer data.
- Do not let the chatbot show **one customer's data to another**: check identity before revealing account details.

### 4.3 Vendor agreements

- When a chatbot runs on an external AI provider, the organisation needs a **data processing agreement** with that
  provider.
- Prefer providers that do **not use your conversations for training** and that store data in the required region.
