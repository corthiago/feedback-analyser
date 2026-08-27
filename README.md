# Project

## Request
```bash
curl -s -X POST "http://localhost:8080/api/interactions" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gemini-3.7-flash",
    "system_instruction": "You are a sentiment analysis assistant. Given customer feedback, respond with the overall sentiment (positive, negative, or neutral) and a one-sentence explanation.",
    "input": "Too few cashiers during peak hours. The store was very crowded during my visit."
  }' | jq .
```

# Response
```json
{
  "created": "2026-08-27T03:04:58Z",
  "errors": null,
  "id": "v1_ChdXcW1QYXZpQ0JfV2dxdHNQX2V1dXNBcxIXV3FtUGF2aUNCX1dncXRzUF9ldXVzQXM",
  "model": "gemini-3.7-flash",
  "object": "interaction",
  "output_text": null,
  "status": "completed",
  "steps": [
    {
      "content": null,
      "type": "thought"
    },
    {
      "content": [
        {
          "text": "**Sentiment:** Negative\n\n**Explanation:** The customer expresses dissatisfaction with inadequate staffing and severe overcrowding during their visit.",
          "type": "text"
        }
      ],
      "type": "model_output"
    }
  ],
  "updated": "2026-08-27T03:04:58Z",
  "usage": {
    "total_input_tokens": 50,
    "total_output_tokens": 22,
    "total_tokens": 202
  }
}
```