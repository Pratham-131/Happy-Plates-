import axios from "axios";

const API_URL = import.meta.env.VITE_API_URL + "/api/orders";

export const fetchUserOrders = async (token) => {
    try {
        const response = await axios.get(API_URL, {
            headers: { Authorization: `Bearer ${token}` },
        }); 
        return response.data;
    } catch (error) {
        console.error('Error occured while fetching the orders', error);
        throw error;
    }
}

export const createOrder = async (orderData, token) => {
    try {
        const response = await axios.post(
            API_URL+"/create",
            orderData,
            { headers: { Authorization: `Bearer ${token}` } }
        );
        return response.data;
    } catch (error) {
        console.error('Error occured while creating the order', error);
        throw error;
    }
}

export const verifyPayment = async (paymentData, token) => {
    try {
        const response = await axios.post(
            API_URL+"/verify",
            paymentData,
            { headers: { Authorization: `Bearer ${token}` } }
        );
        return response.status === 200;
    } catch (error) {
        console.error('Error occured while verifing the payment', error);
        throw error;
    }
}

export const deleteOrder = async (orderId, token) => {
    try {
        await axios.delete(API_URL+"/"+ orderId, {
            headers: { Authorization: `Bearer ${token}` },
        });
    } catch (error) {
        console.error('Error occured while deleting the order', error);
        throw error;
    }
}

export const fetchAllOrders = async (token) => {
    try {
        const response = await axios.get(API_URL+"/all", {
            headers: { Authorization: `Bearer ${token}` },
        });
        return response.data;
    } catch (error) {
        console.error('Error occured while fetching all orders', error);
        throw error;
    }
}
export const updateOrderStatus = async (orderId, status, token) => {
    try {
        await axios.patch(
            API_URL+"/status/"+orderId+"?status="+status,
            {},
            { headers: { Authorization: `Bearer ${token}` } }
        );
    } catch (error) {
        console.error('Error occured while updating order status', error);
        throw error;
    }
}