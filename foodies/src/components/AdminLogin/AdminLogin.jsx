import React, { useContext, useState } from "react";
import { useNavigate } from "react-router-dom";
import { adminLogin } from "../../service/authService";
import { StoreContext } from "../../context/StoreContext";
import { toast } from "react-toastify";

const AdminLogin = () => {
  const { setToken, setRole } = useContext(StoreContext);
  const navigate = useNavigate();
  const [data, setData] = useState({ email: "", password: "" });

  const onChangeHandler = (event) => {
    const { name, value } = event.target;
    setData((data) => ({ ...data, [name]: value }));
  };

  const onSubmitHandler = async (event) => {
    event.preventDefault();
    try {
      const response = await adminLogin(data);
      if (response.status === 200) {
        setToken(response.data.token);
        setRole(response.data.role);
        localStorage.setItem("token", response.data.token);
        localStorage.setItem("role", response.data.role);
        navigate("/admin/foods");
      } else {
        toast.error("Unable to login. Please try again.");
      }
    } catch (error) {
      console.log("Unable to login", error);
      toast.error("Invalid admin credentials");
    }
  };

  return (
    <div className="login-container">
      <div className="row">
        <div className="col-sm-9 col-md-7 col-lg-5 mx-auto">
          <div className="card border-0 shadow rounded-3 my-5">
            <div className="card-body p-4 p-sm-5">
              <h5 className="card-title text-center mb-5 fw-light fs-5">
                Admin Sign In
              </h5>
              <form onSubmit={onSubmitHandler}>
                <div className="form-floating mb-3">
                  <input
                    type="email"
                    className="form-control"
                    id="floatingInput"
                    placeholder="name@example.com"
                    name="email"
                    onChange={onChangeHandler}
                    value={data.email}
                  />
                  <label htmlFor="floatingInput">Admin Email</label>
                </div>
                <div className="form-floating mb-3">
                  <input
                    type="password"
                    className="form-control"
                    id="floatingPassword"
                    placeholder="Password"
                    name="password"
                    onChange={onChangeHandler}
                    value={data.password}
                  />
                  <label htmlFor="floatingPassword">Password</label>
                </div>
                <div className="d-grid">
                  <button
                    className="btn btn-outline-primary btn-login text-uppercase"
                    type="submit"
                  >
                    Sign in
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminLogin;