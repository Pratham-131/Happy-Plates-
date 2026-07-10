import React, { useContext, useState } from "react";
import { StoreContext } from "../../context/StoreContext";
import { addFood, deleteFood, updateFood } from "../../service/foodService";
import { toast } from "react-toastify";

const AdminFoods = () => {
  const { foodList, token, refreshFoodList } = useContext(StoreContext);

  const [formData, setFormData] = useState({
    name: "",
    description: "",
    price: "",
    category: "",
    rating: "",
  });
  const [imageFile, setImageFile] = useState(null);
  const [editingId, setEditingId] = useState(null);

  const onChangeHandler = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const resetForm = () => {
    setFormData({ name: "", description: "", price: "", category: "", rating: "" });
    setImageFile(null);
    setEditingId(null);
  };

  const onSubmitHandler = async (e) => {
    e.preventDefault();
    const payload = {
      name: formData.name,
      description: formData.description,
      price: parseFloat(formData.price),
      category: formData.category,
      rating: parseFloat(formData.rating),
    };
    try {
      if (editingId) {
        await updateFood(editingId, payload, imageFile, token);
        toast.success("Food updated successfully");
      } else {
        if (!imageFile) {
          toast.error("Please select an image");
          return;
        }
        await addFood(payload, imageFile, token);
        toast.success("Food added successfully");
      }
      resetForm();
      await refreshFoodList();
    } catch (error) {
      toast.error("Operation failed. Check console for details.");
      console.error(error);
    }
  };

  const onEditHandler = (food) => {
    setEditingId(food.id);
    setFormData({
      name: food.name,
      description: food.description,
      price: food.price,
      category: food.category,
      rating: food.rating,
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const onDeleteHandler = async (id) => {
    if (!window.confirm("Delete this food item?")) return;
    try {
      await deleteFood(id, token);
      toast.success("Food deleted successfully");
      await refreshFoodList();
    } catch (error) {
      toast.error("Delete failed");
      console.error(error);
    }
  };

  return (
    <div className="container my-4">
      <h3 className="mb-4">{editingId ? "Edit Food Item" : "Add Food Item"}</h3>
      <form onSubmit={onSubmitHandler} className="row g-3 mb-5">
        <div className="col-md-4">
          <input
            className="form-control"
            name="name"
            placeholder="Name"
            value={formData.name}
            onChange={onChangeHandler}
            required
          />
        </div>
        <div className="col-md-4">
          <input
            className="form-control"
            name="category"
            placeholder="Category"
            value={formData.category}
            onChange={onChangeHandler}
            required
          />
        </div>
        <div className="col-md-4">
          <input
            className="form-control"
            name="price"
            type="number"
            step="0.01"
            placeholder="Price"
            value={formData.price}
            onChange={onChangeHandler}
            required
          />
        </div>
        <div className="col-md-8">
          <input
            className="form-control"
            name="description"
            placeholder="Description"
            value={formData.description}
            onChange={onChangeHandler}
            required
          />
        </div>
        <div className="col-md-4">
          <input
            className="form-control"
            name="rating"
            type="number"
            step="0.1"
            min="0"
            max="5"
            placeholder="Rating (0-5)"
            value={formData.rating}
            onChange={onChangeHandler}
            required
          />
        </div>
        <div className="col-md-6">
          <input
            className="form-control"
            type="file"
            accept="image/*"
            onChange={(e) => setImageFile(e.target.files[0])}
          />
        </div>
        <div className="col-md-6 d-flex gap-2">
          <button type="submit" className="btn btn-primary">
            {editingId ? "Update Food" : "Add Food"}
          </button>
          {editingId && (
            <button type="button" className="btn btn-secondary" onClick={resetForm}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <h3 className="mb-3">Manage Food Items</h3>
      <div className="table-responsive">
        <table className="table table-striped align-middle">
          <thead>
            <tr>
              <th>Image</th>
              <th>Name</th>
              <th>Category</th>
              <th>Price</th>
              <th>Rating</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {foodList.map((food) => (
              <tr key={food.id}>
                <td>
                  <img src={food.imageUrl} alt={food.name} width={60} height={60} style={{ objectFit: "cover" }} />
                </td>
                <td>{food.name}</td>
                <td>{food.category}</td>
                <td>&#8377;{food.price}</td>
                <td>{food.rating}</td>
                <td className="d-flex gap-2">
                  <button className="btn btn-sm btn-outline-primary" onClick={() => onEditHandler(food)}>
                    Edit
                  </button>
                  <button className="btn btn-sm btn-outline-danger" onClick={() => onDeleteHandler(food.id)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default AdminFoods;