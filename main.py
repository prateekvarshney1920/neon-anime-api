from fastapi import FastAPI, UploadFile, File
from fastapi.responses import JSONResponse
import requests
import base64
import os

app = FastAPI(title="NeonAnime Backend")

# Free API Keys (Store these in .env files in production!)
HUGGING_FACE_TOKEN = "your_hf_token_here"
GEMINI_API_KEY = "your_gemini_token_here"

# We use a popular, fast Anime model on Hugging Face
MODEL_URL = "https://api-inference.huggingface.co/models/proximasanfinetuning/lcm_lora_animagine_xl_3.1"

@app.post("/process-media")
async def process_media(file: UploadFile = File(...)):
    try:
        # 1. Read the incoming image from the Android App
        image_bytes = await file.read()

        # 2. Call Hugging Face API for Image Transformation
        headers = {"Authorization": f"Bearer {HUGGING_FACE_TOKEN}"}
        
        # Note: You might need to adjust payload depending on the specific model
        response = requests.post(MODEL_URL, headers=headers, data=image_bytes)
        
        if response.status_code != 200:
            return JSONResponse(status_code=500, content={"error": "AI processing failed"})

        processed_image_bytes = response.content
        
        # Convert to Base64 to send back to Android safely in JSON
        base64_image = base64.b64encode(processed_image_bytes).decode('utf-8')

        # 3. Generate the Instagram Caption (Mocked here, but you'd call Gemini API)
        caption = "Caught in the digital crossfire. ⚡️ #NeonAnime #CyberpunkAesthetic #DigitalArt"

        # 4. Return the payload to the Android app
        return {
            "status": "success",
            "image_base64": base64_image,
            "caption": caption
        }

    except Exception as e:
        return JSONResponse(status_code=500, content={"error": str(e)})

# Run with: uvicorn main:app --reload