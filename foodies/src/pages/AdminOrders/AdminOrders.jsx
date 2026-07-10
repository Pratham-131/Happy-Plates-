import React, { useContext, useEffect, useState } from "react";
import { StoreContext } from "../../context/StoreContext";
import { fetchAllOrders, updateOrderStatus } from "../../service/orderService";
import { toast } from "react-toastify";

const STATUS_OPTIONS = ["Preparing", "Out for delivery", "Delivered", "Cancelled"];

const AdminOrders = () => {
  const { token } = useContext(StoreContext);
  const [orders, setOrders] = useState([]);

  const fetchOrders = async () => {
    try {
      const response = await fetchAllOrders(token);
      setOrders(response);
    } catch (error) {
      toast.error("Failed to fetch orders");
      console.error(error);
    }
  };

  useEffect(() => {
    if (token) {
      fetchOrders();
    }
  }, [token]);

  const onStatusChange = async (orderId, newStatus) => {
    try {
      await updateOrderStatus(orderId, newStatus, token);
      toast.success("Order status updated");
      await fetchOrders();
    } catch (error) {
      toast.error("Failed to update status");
      console.error(error);
    }
  };

  return (
    <div className="container my-4">
      <h3 className="mb-4">Manage Orders</h3>
      <div className="table-responsive">
        <table className="table table-striped align-middle">
          <thead>
            <tr>
              <th>Customer</th>
              <th>Contact</th>
              <th>Address</th>
              <th>Items</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Update Status</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order, index) => (
              <tr key={index}>
                <td>{order.email}</td>
                <td>{order.phoneNumber}</td>
                <td style={{ maxWidth: "220px" }}>{order.userAddress}</td>
                <td>
                  {order.orderedItems.map((item, i) =>
                    i === order.orderedItems.length - 1
                      ? `${item.name} x ${item.quantity}`
                      : `${item.name} x ${item.quantity}, `
                  )}
                </td>
                <td>&#8377;{order.amount.toFixed(2)}</td>
                <td className="fw-bold text-capitalize">{order.orderStatus}</td>
                <td>
                  <select
                    className="form-select form-select-sm"
                    value={order.orderStatus}
                    onChange={(e) => onStatusChange(order.id, e.target.value)}
                  >
                    {STATUS_OPTIONS.map((status) => (
                      <option key={status} value={status}>
                        {status}
                      </option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default AdminOrders;
