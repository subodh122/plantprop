import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

export interface PlantAnalysis {
  commonName: string;
  scientificName: string;
  confidence: number;
  summary: string;
  propagationMethods: string;
  tips: string;
}

export async function analyzePlant(base64Image: string): Promise<PlantAnalysis> {
  const prompt = `Analyze this image of a plant or tree. 
  Identify the plant (common and scientific name) and provide comprehensive propagation information.
  
  IMPORTANT: Return your response ONLY as a JSON object with this exact structure:
  {
    "commonName": "string",
    "scientificName": "string",
    "confidence": number (between 0 and 1, representing your estimation of identity accuracy),
    "summary": "string (1-2 sentences)",
    "propagationMethods": "string (Markdown formatted steps)",
    "tips": "string (Markdown formatted pro tips)"
  }
  
  Do not include any extra text before or after the JSON.`;

  try {
    const response = await ai.models.generateContent({
      model: "gemini-3-flash-preview",
      contents: [
        {
          parts: [
            { text: prompt },
            {
              inlineData: {
                mimeType: "image/jpeg",
                data: base64Image.split(",")[1],
              },
            },
          ],
        },
      ],
      config: {
        responseMimeType: "application/json",
      }
    });

    const text = response.text;
    if (!text) throw new Error("No response from AI.");
    
    return JSON.parse(text) as PlantAnalysis;
  } catch (error) {
    console.error("Gemini API Error:", error);
    throw new Error("Failed to connect to the botanical wisdom engine.");
  }
}
