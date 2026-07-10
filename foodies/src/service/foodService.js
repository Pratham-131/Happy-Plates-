import axios from "axios";
const API_URL = import.meta.env.VITE_API_URL + "/api/foods";
export const fetchFoodList = async () => {
    try {
        const response = await axios.get(API_URL);
        return response.data;
    } catch (error) {
        console.log('Error fetching food list:', error);
        throw error;
    }    
}
export const fetchFoodDetails = async (id) => {
    try {
        const response = await axios.get(API_URL+"/"+id);
        return response.data;
    } catch (error) {
        console.log('Error fetching food details:', error);
        throw error;
    }
    
}
export const addFood = async (foodData, imageFile, token) => {
    try {
        const formData = new FormData();
        formData.append("food", JSON.stringify(foodData));
        formData.append("file", imageFile);
        const response = await axios.post(API_URL, formData, {
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "multipart/form-data",
            },
        });
        return response.data;
    } catch (error) {
        console.log('Error adding food:', error);
        throw error;
    }
}
export const updateFood = async (id, foodData, imageFile, token) => {
    try {
        const formData = new FormData();
        formData.append("food", JSON.stringify(foodData));
        if (imageFile) {
            formData.append("file", imageFile);
        }
        const response = await axios.put(API_URL+"/"+id, formData, {
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "multipart/form-data",
            },
        });
        return response.data;
    } catch (error) {
        console.log('Error updating food:', error);
        throw error;
    }
}
export const deleteFood = async (id, token) => {
    try {
        await axios.delete(API_URL+"/"+id, {
            headers: { Authorization: `Bearer ${token}` },
        });
    } catch (error) {
        console.log('Error deleting food:', error);
        throw error;
    }
}