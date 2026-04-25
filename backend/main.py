from fastapi import FastAPI, File, UploadFile
from fastapi.responses import Response
import io
from PIL import Image, ImageEnhance, ImageOps

app = FastAPI(title="NeonAnime Backend")

def apply_neon_filter(image_bytes: bytes) -> bytes:
    # Open the image using Pillow
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    
    # Simple "Neon/Cyberpunk" simulated effect
    # 1. Increase contrast
    enhancer = ImageEnhance.Contrast(image)
    image = enhancer.enhance(1.5)
    
    # 2. Increase color saturation
    color_enhancer = ImageEnhance.Color(image)
    image = color_enhancer.enhance(1.8)
    
    # 3. Apply a slight pink/cyan tint (by splitting channels and merging)
    r, g, b = image.split()
    # Boost Red and Blue for a pink/purple tint
    r = r.point(lambda i: min(255, int(i * 1.2)))
    b = b.point(lambda i: min(255, int(i * 1.3)))
    # Slightly reduce Green
    g = g.point(lambda i: int(i * 0.8))
    
    image = Image.merge("RGB", (r, g, b))
    
    # Save processed image back to bytes
    output = io.BytesIO()
    # Convert to JPEG for output
    image.save(output, format="JPEG", quality=90)
    return output.getvalue()

@app.post("/process-image")
async def process_image(image: UploadFile = File(...)):
    # Read the uploaded image bytes
    contents = await image.read()
    
    # Process the image
    processed_bytes = apply_neon_filter(contents)
    
    # Return the processed image directly as a JPEG file
    return Response(content=processed_bytes, media_type="image/jpeg")

@app.get("/")
def read_root():
    return {"message": "NeonAnime API is running! Use POST /process-image to process an image."}
